package com.example.library.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.library.auth.domain.RefreshToken;
import com.example.library.auth.domain.RefreshTokenRepository;
import com.example.library.common.crypto.Hashing;
import com.example.library.support.Concurrently;
import com.example.library.support.IntegrationTestSupport;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 같은 Refresh 토큰을 동시에 제시했을 때 재사용 탐지가 무력화되지 않는지 검증한다.
 */
class RefreshConcurrencyIntegrationTest extends IntegrationTestSupport {

	private static final int ROUNDS = 5;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Test
	@DisplayName("같은 Refresh 토큰으로 동시에 재발급하면 하나만 200, 나머지는 401 이고 승자의 새 토큰까지 폐기된다")
	void concurrentRefreshWithSameToken() throws Exception {
		for (int round = 0; round < ROUNDS; round++) {
			String refreshToken = login("member", MEMBER_PASSWORD).refreshToken();

			List<MockHttpServletResponse> responses = Concurrently.run(List.of(
					() -> postRefresh(refreshToken),
					() -> postRefresh(refreshToken)));

			assertThat(responses).extracting(MockHttpServletResponse::getStatus).containsExactlyInAnyOrder(200, 401);
			MockHttpServletResponse winner = responses.stream().filter(r -> r.getStatus() == 200).findFirst().orElseThrow();
			String issued = objectMapper.readValue(winner.getContentAsString(), TokenResponse.class).refreshToken();
			assertThat(postRefresh(issued).getStatus()).isEqualTo(401);
		}
	}

	@Test
	@DisplayName("조건부 폐기는 살아 있는 토큰에만 한 번 적용된다(1건 → 0건)")
	void revokeIfActiveAppliesOnce() throws Exception {
		String raw = login("member", MEMBER_PASSWORD).refreshToken();
		RefreshToken token = refreshTokenRepository.findByTokenHash(Hashing.sha256Hex(raw)).orElseThrow();
		LocalDateTime now = LocalDateTime.now();

		assertThat(revokeIfActive(token.getId(), now)).isEqualTo(1);
		assertThat(revokeIfActive(token.getId(), now)).isZero();
	}

	private int revokeIfActive(Long id, LocalDateTime now) {
		return transactionTemplate.execute(tx -> refreshTokenRepository.revokeIfActive(id, now));
	}

	private MockHttpServletResponse postRefresh(String refreshToken) throws Exception {
		return mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
				.andReturn().getResponse();
	}
}
