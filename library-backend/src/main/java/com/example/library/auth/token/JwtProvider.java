package com.example.library.auth.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Access JWT 발급·검증.
 * 토큰에는 subject(userId)만 넣는다 — 권한·접속 조건은 매 요청 캐시에서 읽어 즉시 회수가 가능하게 한다.
 */
@Component
public class JwtProvider {

	private final SecretKey key;
	private final JwtProperties properties;
	private final Clock clock;
	private final JwtParser parser;

	public JwtProvider(JwtProperties properties, Clock clock) {
		this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
		this.properties = properties;
		this.clock = clock;
		this.parser = Jwts.parser()
				.verifyWith(key)
				.requireIssuer(properties.issuer())
				.clock(() -> Date.from(clock.instant()))
				.build();
	}

	public String createAccessToken(Long userId) {
		Instant now = clock.instant();
		return Jwts.builder()
				.issuer(properties.issuer())
				.subject(String.valueOf(userId))
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(properties.accessTokenTtl())))
				.signWith(key)
				.compact();
	}

	public long accessTokenTtlSeconds() {
		return properties.accessTokenTtl().toSeconds();
	}

	/**
	 * 토큰을 검증한다.
	 * jjwt 는 검증 실패를 예외로만 알려 주므로, 라이브러리 경계인 이곳에서만 결과 타입으로 변환한다.
	 */
	public AccessTokenResult verify(String token) {
		try {
			Claims claims = parser.parseSignedClaims(token).getPayload();
			return toResult(claims.getSubject());
		} catch (ExpiredJwtException e) {
			return new AccessTokenResult.Expired();
		} catch (JwtException | IllegalArgumentException e) {
			return new AccessTokenResult.Invalid();
		}
	}

	private AccessTokenResult toResult(String subject) {
		if (subject == null || subject.isEmpty() || !subject.chars().allMatch(Character::isDigit)) {
			return new AccessTokenResult.Invalid();
		}
		return new AccessTokenResult.Valid(Long.valueOf(subject));
	}
}
