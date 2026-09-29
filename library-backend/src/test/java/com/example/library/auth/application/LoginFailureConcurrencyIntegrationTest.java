package com.example.library.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.library.support.Concurrently;
import com.example.library.support.IntegrationTestSupport;
import com.example.library.user.domain.User;
import com.example.library.user.domain.UserRepository;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * 동시 로그인 실패에서 실패 횟수가 유실되지 않는지(read-modify-write 경합 방지) 검증한다.
 */
class LoginFailureConcurrencyIntegrationTest extends IntegrationTestSupport {

	private static final String PASSWORD = "Passw0rd!";

	@Autowired
	private LoginAttemptService loginAttemptService;

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("실패 기록 10건을 동시에 넣으면 실패 횟수는 정확히 10 이고 계정은 잠긴다")
	void concurrentFailuresAreAllCounted() throws Exception {
		signup("failrace01", PASSWORD);
		Long userId = userRepository.findByUsername("failrace01").orElseThrow().getId();
		List<Callable<Boolean>> tasks = IntStream.range(0, 10)
				.<Callable<Boolean>>mapToObj(i -> () -> {
					loginAttemptService.recordFailure(userId);
					return true;
				})
				.toList();

		Concurrently.run(tasks);

		User user = userRepository.findById(userId).orElseThrow();
		assertThat(user.getFailedLoginCount()).isEqualTo(10);
		assertThat(user.isLocked()).isTrue();
	}

	@Test
	@DisplayName("한도 미만의 틀린 비밀번호 로그인 4건을 동시에 보내면 4회가 모두 기록되고, 이어진 1회로 잠긴다")
	void concurrentWrongPasswordLoginsBelowLimit() throws Exception {
		signup("failrace02", PASSWORD);
		List<Callable<Integer>> tasks = IntStream.range(0, 4)
				.<Callable<Integer>>mapToObj(i -> () -> wrongLoginStatus("failrace02"))
				.toList();

		assertThat(Concurrently.run(tasks)).containsOnly(401);
		assertThat(userRepository.findByUsername("failrace02").orElseThrow().getFailedLoginCount()).isEqualTo(4);
		assertThat(userRepository.findByUsername("failrace02").orElseThrow().isLocked()).isFalse();

		wrongLoginStatus("failrace02");
		assertThat(userRepository.findByUsername("failrace02").orElseThrow().isLocked()).isTrue();
	}

	private int wrongLoginStatus(String username) throws Exception {
		return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"%s\",\"password\":\"wrong-password\"}".formatted(username)))
				.andReturn().getResponse().getStatus();
	}
}
