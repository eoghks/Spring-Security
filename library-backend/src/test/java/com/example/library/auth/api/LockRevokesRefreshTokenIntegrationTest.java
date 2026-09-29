package com.example.library.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.auth.domain.RefreshTokenRepository;
import com.example.library.common.crypto.Hashing;
import com.example.library.support.IntegrationTestSupport;
import com.example.library.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * 로그인 실패로 계정이 잠기면 그 사용자의 Refresh 토큰이 모두 폐기되는지 검증한다.
 */
class LockRevokesRefreshTokenIntegrationTest extends IntegrationTestSupport {

	private static final String PASSWORD = "Passw0rd!";

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("잠기는 순간 기존 Refresh 토큰이 폐기되어, 관리자가 잠금을 풀어도 옛 토큰으로는 재발급할 수 없다")
	void lockRevokesRefreshTokens() throws Exception {
		signup("lockrevoke01", PASSWORD);
		String refreshToken = login("lockrevoke01", PASSWORD).refreshToken();

		for (int i = 0; i < 5; i++) {
			mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\":\"lockrevoke01\",\"password\":\"bad\"}"));
		}

		assertThat(refreshTokenRepository.findByTokenHash(Hashing.sha256Hex(refreshToken)).orElseThrow().isRevoked())
				.isTrue();
		long userId = userRepository.findByUsername("lockrevoke01").orElseThrow().getId();
		mockMvc.perform(post("/api/admin/users/" + userId + "/unlock").header("Authorization",
						bearer("admin", ADMIN_PASSWORD)))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
	}
}
