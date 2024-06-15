package com.example.SpringSecurity_Example.config;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

	private final Key key;
	private final long expirationTime;

	public JwtUtil(@Value("${jwt.secret}") String secretKey, @Value("${jwt.expiration_time}")long expirationTime) {
		byte[] keyBytes = Decoders.BASE64.decode(secretKey);
		this.key = Keys.hmacShaKeyFor(keyBytes);// byte를 기준으로 Hmac 알괴즘을 적용해 seucrity.Key 생성
		this.expirationTime= expirationTime;
	}

	public String generatedJwtToken(CustomUser cu) {
		long now = (new Date()).getTime();
		return
	}
}
