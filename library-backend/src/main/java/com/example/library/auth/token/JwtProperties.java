package com.example.library.auth.token;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정.
 *
 * @param secret          HMAC 서명 키(Base64)
 * @param issuer          발급자
 * @param accessTokenTtl  Access 토큰 유효기간
 * @param refreshTokenTtl Refresh 토큰 유효기간
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {
}
