package com.example.SpringSecurity_Example.jwt;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.SpringSecurity_Example.model.security.CustomMemberInfoDto;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtUtil {

	private final Key key;
	private final long expirationTime;

	public JwtUtil(@Value("${jwt.secret}") String secretKey, @Value("${jwt.expiration_time}")long expirationTime) {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		this.key = Keys.hmacShaKeyFor(keyBytes);// byte를 기준으로 Hmac 알괴즘을 적용해 seucrity.Key 생성
		this.expirationTime= expirationTime;
	}

	public String generatedJwtToken(CustomMemberInfoDto info) {
		Claims claims = Jwts.claims();
		claims.put("memberId", info.getMemberId());
		claims.put("role", info.getRole());

		Date now = new Date();
		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(now)
				.setExpiration(new Date(now.getTime() + this.expirationTime))
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();
	}

	//Token에서 Claims 추출 후 memberId 추출
	public Long getMemberId(String accessToken) {
		return parseClamins(accessToken).get("memberId", Long.class);
	}

	//JWT 유효성 체크
	public boolean validateToken(String accessToken) {
		try {
			Jwts.parserBuilder().setSigningKey(this.key).build().parseClaimsJws(accessToken).getBody();
			return true;
		} catch (SecurityException | MalformedJwtException e) {
			log.info("Invalid JWT Token", e);
		} catch (ExpiredJwtException e) {
			log.info("Expired JWT Token", e);
		} catch(UnsupportedJwtException e) {
			log.info("Unsupported JWT Token", e);
		} catch(IllegalArgumentException  e) {
			log.info("JWT claims string is empty", e);
		}

		return false;
	}

	//Claim 정보 추출
	private Claims parseClamins(String accessToken) {
		try {
			return Jwts.parserBuilder().setSigningKey(this.key).build().parseClaimsJws(accessToken).getBody();
		} catch(ExpiredJwtException e) {
			return e.getClaims();
		}
	}
}
