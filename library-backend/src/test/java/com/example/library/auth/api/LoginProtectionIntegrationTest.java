package com.example.library.auth.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.config.LoginProtectionProperties;
import com.example.library.security.LoginFailureLimiter;
import com.example.library.support.IntegrationTestSupport;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 로그인 응답이 계정 상태를 드러내지 않는지, IP 단위 로그인 실패 제한이 동작하는지 검증한다.
 */
class LoginProtectionIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private LoginFailureLimiter loginFailureLimiter;

	@Autowired
	private LoginProtectionProperties loginProtectionProperties;

	@Test
	@DisplayName("잠긴 계정이라도 틀린 비밀번호에는 INVALID_CREDENTIALS, 올바른 비밀번호에만 ACCOUNT_LOCKED")
	void lockedAccountIsDisclosedOnlyWithCorrectPassword() throws Exception {
		signup("lockhide01", "Passw0rd!");
		for (int i = 0; i < 5; i++) {
			login("lockhide01", "bad", "127.0.0.1");
		}

		login("lockhide01", "bad", "127.0.0.1")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
		login("lockhide01", "Passw0rd!", "127.0.0.1")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
	}

	@Test
	@DisplayName("한 IP 의 로그인 실패가 한도에 도달하면 그 분 동안 올바른 비밀번호도 429, 다른 IP 는 영향이 없다")
	void blocksIpAfterTooManyFailures() throws Exception {
		// 분 경계를 넘어 버킷이 바뀌면 판정이 흔들리므로, 같은 분 안에서 끝난 시도로만 검증한다
		for (int attempt = 0; attempt < 2; attempt++) {
			long minute = currentMinute();
			String ip = "203.0.113." + (50 + attempt);
			fillFailuresUntilOneLeft(ip);
			login("member", "wrong-password", ip).andExpect(status().isUnauthorized());
			ResultActions blocked = login("member", MEMBER_PASSWORD, ip);
			if (minute == currentMinute()) {
				blocked.andExpect(status().isTooManyRequests())
						.andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
				login("member", MEMBER_PASSWORD, "203.0.113.99").andExpect(status().isOk());
				return;
			}
		}
		throw new AssertionError("두 번 연속 분 경계에 걸려 검증하지 못했다");
	}

	private void fillFailuresUntilOneLeft(String ip) {
		for (int i = 0; i < loginProtectionProperties.maxFailuresPerMinute() - 1; i++) {
			loginFailureLimiter.recordFailure(ip);
		}
	}

	private long currentMinute() {
		return Instant.now().getEpochSecond() / 60;
	}

	private ResultActions login(String username, String password, String remoteAddr) throws Exception {
		return mockMvc.perform(post("/api/auth/login")
				.with(request -> {
					request.setRemoteAddr(remoteAddr);
					return request;
				})
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)));
	}
}
