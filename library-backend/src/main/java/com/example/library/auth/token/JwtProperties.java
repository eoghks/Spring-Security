package com.example.library.auth.token;

import io.jsonwebtoken.io.Decoders;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정. 서명 키가 안전하지 않으면 바인딩 단계에서 예외를 던져 기동을 멈춘다(fail-fast).
 * <ul>
 *   <li>키가 비어 있으면 거부 — 기본(H2) 개발 프로필 밖에서는 환경 변수 JWT_SECRET 이 필수다</li>
 *   <li>공개 저장소에 있는 개발용 키는 개발 프로필(devSecretAllowed=true)에서만 허용</li>
 *   <li>HS256 기준 디코드 결과가 32바이트 미만이면 거부</li>
 * </ul>
 *
 * @param secret           HMAC 서명 키(Base64)
 * @param issuer           발급자
 * @param accessTokenTtl   Access 토큰 유효기간
 * @param refreshTokenTtl  Refresh 토큰 유효기간
 * @param devSecretAllowed 개발용 키 허용 여부(application-default.yml 에서만 true)
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl,
		boolean devSecretAllowed) {

	/** 공개 저장소에 올라가 있는 개발 전용 키 — 기본(H2) 개발 프로필 밖에서는 쓸 수 없다 */
	public static final String DEV_ONLY_SECRET = "bGlicmFyeS1kZXYtb25seS1qd3Qtc2VjcmV0LWtleS1jaGFuZ2UtbWUtaW4tcHJvZA==";

	/** HS256 최소 키 길이(바이트) */
	public static final int MIN_SECRET_BYTES = 32;

	public JwtProperties {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("JWT 서명 키(app.jwt.secret)가 없습니다. 환경 변수 JWT_SECRET 을 설정하세요.");
		}
		if (DEV_ONLY_SECRET.equals(secret) && !devSecretAllowed) {
			throw new IllegalStateException("개발용 JWT 서명 키는 기본(H2) 개발 프로필에서만 쓸 수 있습니다. JWT_SECRET 을 설정하세요.");
		}
		if (Decoders.BASE64.decode(secret).length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"JWT 서명 키가 너무 짧습니다. Base64 디코드 기준 " + MIN_SECRET_BYTES + "바이트 이상이어야 합니다.");
		}
	}
}
