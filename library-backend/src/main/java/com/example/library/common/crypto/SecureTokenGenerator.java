package com.example.library.common.crypto;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * 암호학적으로 안전한 무작위 토큰 생성기(Refresh 토큰, API Key 원문).
 */
@Component
public class SecureTokenGenerator {

	private static final int TOKEN_BYTES = 32;

	private final SecureRandom secureRandom = new SecureRandom();

	/** 32바이트 무작위 값을 패딩 없는 base64url 문자열로 만든다 */
	public String generate() {
		byte[] bytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
