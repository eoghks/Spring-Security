package com.example.library.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.library.support.Concurrently;
import com.example.library.support.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;

/**
 * 유니크 제약 경합이 로그에 입력값(가입 아이디)을 남기지 않는지 검증한다.
 */
@ExtendWith(OutputCaptureExtension.class)
class ConstraintLogPrivacyIntegrationTest extends IntegrationTestSupport {

	@Test
	@DisplayName("같은 아이디 동시 가입으로 유니크 제약에 걸려도 로그에 그 아이디가 찍히지 않는다")
	void concurrentSignupDoesNotLogUsername(CapturedOutput output) {
		for (int round = 0; round < 3; round++) {
			String username = "leakprobe0" + round;
			String body = """
					{"username":"%s","password":"Passw0rd!","name":"경합","email":"%s@library.local"}
					""".formatted(username, username);

			List<Integer> statuses = Concurrently.run(List.of(() -> signupStatus(body), () -> signupStatus(body)));

			assertThat(statuses).containsExactlyInAnyOrder(201, 409);
		}
		assertThat(output).doesNotContain("leakprobe0");
	}

	private int signupStatus(String body) throws Exception {
		return mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
				.andReturn().getResponse().getStatus();
	}
}
