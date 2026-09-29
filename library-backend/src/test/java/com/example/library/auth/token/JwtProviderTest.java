package com.example.library.auth.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

	private static final String SECRET = "dGVzdC1vbmx5LWp3dC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLTEyMzQ1Njc4";
	private static final JwtProperties PROPERTIES =
			new JwtProperties(SECRET, "test-issuer", Duration.ofMinutes(15), Duration.ofDays(7));
	private static final Instant NOW = Instant.parse("2026-01-05T00:00:00Z");

	@Test
	@DisplayName("발급한 토큰은 subject 로 userId 를 돌려준다")
	void createAndVerify() {
		JwtProvider provider = new JwtProvider(PROPERTIES, Clock.fixed(NOW, ZoneOffset.UTC));

		AccessTokenResult result = provider.verify(provider.createAccessToken(42L));

		assertThat(result).isEqualTo(new AccessTokenResult.Valid(42L));
	}

	@Test
	@DisplayName("15분이 지나면 만료 결과를 돌려준다")
	void expired() {
		String token = new JwtProvider(PROPERTIES, Clock.fixed(NOW, ZoneOffset.UTC)).createAccessToken(1L);
		JwtProvider later = new JwtProvider(PROPERTIES, Clock.fixed(NOW.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));

		assertThat(later.verify(token)).isInstanceOf(AccessTokenResult.Expired.class);
	}

	@Test
	@DisplayName("다른 키로 서명된 토큰·형식 오류 토큰은 무효 결과를 돌려준다")
	void invalid() {
		JwtProperties other = new JwtProperties(
				"b3RoZXItdGVzdC1qd3Qtc2VjcmV0LWtleS1mb3ItdW5pdC10ZXN0cy0xMjM0NTY3OA==", "test-issuer",
				Duration.ofMinutes(15), Duration.ofDays(7));
		String foreign = new JwtProvider(other, Clock.fixed(NOW, ZoneOffset.UTC)).createAccessToken(1L);
		JwtProvider provider = new JwtProvider(PROPERTIES, Clock.fixed(NOW, ZoneOffset.UTC));

		assertThat(provider.verify(foreign)).isInstanceOf(AccessTokenResult.Invalid.class);
		assertThat(provider.verify("not-a-jwt")).isInstanceOf(AccessTokenResult.Invalid.class);
	}
}
