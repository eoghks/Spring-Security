package com.example.library.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.auth.token.AccessTokenResult;
import com.example.library.auth.token.JwtProvider;
import com.example.library.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class AuthIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private JwtProvider jwtProvider;

	@Test
	@DisplayName("로그인하면 subject 만 담긴 Access 토큰과 Refresh 토큰을 받는다")
	void loginIssuesTokens() throws Exception {
		TokenResponse tokens = login("member", MEMBER_PASSWORD);

		assertThat(tokens.tokenType()).isEqualTo("Bearer");
		assertThat(tokens.expiresIn()).isEqualTo(15 * 60);
		assertThat(tokens.refreshToken()).isNotBlank();
		assertThat(jwtProvider.verify(tokens.accessToken())).isInstanceOf(AccessTokenResult.Valid.class);
	}

	@Test
	@DisplayName("비밀번호가 틀리거나 없는 아이디면 같은 401 INVALID_CREDENTIALS")
	void invalidCredentials() throws Exception {
		postLogin("member", "wrong-password")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
		postLogin("nobody", "wrong-password")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	@DisplayName("로그인 5회 실패 시 계정이 잠기고 올바른 비밀번호로도 로그인할 수 없다")
	void lockAfterFiveFailures() throws Exception {
		signup("locktest01", "Passw0rd!");
		for (int i = 0; i < 4; i++) {
			postLogin("locktest01", "bad").andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
		}
		postLogin("locktest01", "bad").andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));

		postLogin("locktest01", "Passw0rd!")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
	}

	@Test
	@DisplayName("재발급하면 Refresh 토큰이 회전되고, 옛 토큰 재사용 시 새 토큰까지 모두 폐기된다")
	void refreshRotationAndReuseDetection() throws Exception {
		TokenResponse first = login("member", MEMBER_PASSWORD);

		TokenResponse second = readTokens(postRefresh(first.refreshToken()).andExpect(status().isOk()));
		assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());

		postRefresh(first.refreshToken())
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
		postRefresh(second.refreshToken()).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("로그아웃하면 Refresh 토큰이 폐기되어 재발급할 수 없다")
	void logoutRevokesRefreshToken() throws Exception {
		TokenResponse tokens = login("member", MEMBER_PASSWORD);

		mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
						.content(refreshBody(tokens.refreshToken())))
				.andExpect(status().isNoContent());

		postRefresh(tokens.refreshToken()).andExpect(status().isUnauthorized());
	}

	private ResultActions postLogin(String username, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)));
	}

	private ResultActions postRefresh(String refreshToken) throws Exception {
		return mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody(refreshToken)));
	}

	private String refreshBody(String refreshToken) {
		return "{\"refreshToken\":\"%s\"}".formatted(refreshToken);
	}

	private TokenResponse readTokens(ResultActions actions) throws Exception {
		return objectMapper.readValue(actions.andReturn().getResponse().getContentAsString(), TokenResponse.class);
	}
}
