package com.example.library.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.auth.token.JwtProperties;
import com.example.library.auth.token.JwtProvider;
import com.example.library.support.IntegrationTestSupport;
import com.example.library.user.domain.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SecurityFilterChainIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private JwtProperties jwtProperties;

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("토큰 없이 보호 URL 을 호출하면 401 UNAUTHORIZED")
	void noToken() throws Exception {
		mockMvc.perform(get("/api/books"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("만료된 토큰은 401 TOKEN_EXPIRED")
	void expiredToken() throws Exception {
		Clock past = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
		String expired = new JwtProvider(jwtProperties, past).createAccessToken(memberId());

		mockMvc.perform(get("/api/books").header("Authorization", "Bearer " + expired))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
	}

	@Test
	@DisplayName("형식이 잘못된 토큰은 401 INVALID_TOKEN")
	void invalidToken() throws Exception {
		mockMvc.perform(get("/api/books").header("Authorization", "Bearer garbage"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
	}

	@Test
	@DisplayName("로그인한 사용자라도 미등록 URL 은 403 ACCESS_DENIED (fail-closed)")
	void unregisteredUrl() throws Exception {
		String token = jwtProvider.createAccessToken(memberId());

		mockMvc.perform(get("/api/not-registered").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	private Long memberId() {
		return userRepository.findByUsername("member").orElseThrow().getId();
	}
}
