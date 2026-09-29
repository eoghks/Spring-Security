package com.example.library.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.support.IntegrationTestSupport;
import com.example.library.user.domain.User;
import com.example.library.user.domain.UserRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

class SignupIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Test
	@DisplayName("회원가입하면 MEMBER 역할이 부여되고 비밀번호는 BCrypt 해시로 저장된다")
	void signup() throws Exception {
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
						{"username":"newbie01","password":"Passw0rd!","name":"신입","email":"newbie@library.local"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("newbie01"));

		transactionTemplate.executeWithoutResult(tx -> {
			User user = userRepository.findByUsername("newbie01").orElseThrow();
			assertThat(user.getRole().getCode()).isEqualTo("MEMBER");
			assertThat(user.getPassword()).startsWith("$2a$").isNotEqualTo("Passw0rd!");
		});
	}

	@Test
	@DisplayName("검증 실패 시 400 과 필드별 메시지를 돌려준다")
	void validationErrors() throws Exception {
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
						{"username":"A!","password":"short","name":"","email":"not-email"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors[?(@.field=='username')]").exists())
				.andExpect(jsonPath("$.fieldErrors[?(@.field=='password')]").exists())
				.andExpect(jsonPath("$.fieldErrors[?(@.field=='email')]").exists());
	}

	@Test
	@DisplayName("이미 사용 중인 아이디면 409 DUPLICATE_USERNAME")
	void duplicate() throws Exception {
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
						{"username":"member","password":"Passw0rd!","name":"중복","email":"dup@library.local"}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_USERNAME"));
	}

	@Test
	@DisplayName("같은 아이디로 동시에 가입하면 하나만 201, 나머지는 409 DUPLICATE_USERNAME — 500 은 없다")
	void concurrentSignupSameUsername() throws Exception {
		for (int round = 0; round < 5; round++) {
			String username = "racer0" + round;
			List<Integer> statuses = signupConcurrently(username, 2);

			assertThat(statuses).containsExactlyInAnyOrder(201, 409);
			assertThat(userRepository.findByUsername(username)).isPresent();
		}
	}

	/** 준비 신호에 맞춰 여러 스레드가 같은 아이디로 동시에 가입 요청을 보낸다 */
	private List<Integer> signupConcurrently(String username, int threads) throws Exception {
		String body = """
				{"username":"%s","password":"Passw0rd!","name":"경합","email":"%s@library.local"}
				""".formatted(username, username);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<CompletableFuture<Integer>> futures = IntStream.range(0, threads)
				.mapToObj(i -> CompletableFuture.supplyAsync(() -> postSignup(body, start), executor))
				.toList();
		start.countDown();
		List<Integer> statuses = futures.stream().map(CompletableFuture::join).toList();
		executor.shutdown();
		return statuses;
	}

	private int postSignup(String body, CountDownLatch start) {
		try {
			start.await();
			return mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
					.andReturn().getResponse().getStatus();
		} catch (Exception e) {
			// 테스트 스레드 안의 검사 예외를 호출 스레드로 넘긴다(흐름 제어 목적 아님)
			throw new IllegalStateException(e);
		}
	}
}
