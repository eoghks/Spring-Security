package com.example.library.auth.token;

/**
 * Access 토큰 검증 결과.
 */
public sealed interface AccessTokenResult {

	/** 유효한 토큰 */
	record Valid(Long userId) implements AccessTokenResult {
	}

	/** 서명은 맞지만 만료된 토큰 */
	record Expired() implements AccessTokenResult {
	}

	/** 서명 불일치·형식 오류 등 */
	record Invalid() implements AccessTokenResult {
	}
}
