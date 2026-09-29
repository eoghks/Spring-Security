package com.example.library.auth.api;

/**
 * 토큰 발급 응답.
 *
 * @param accessToken  Access JWT
 * @param refreshToken Refresh 토큰 원문(이 응답에서만 전달)
 * @param tokenType    토큰 유형(Bearer)
 * @param expiresIn    Access 토큰 유효기간(초)
 */
public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {

	public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn) {
		return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
	}

	@Override
	public String toString() {
		return "TokenResponse[tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
	}
}
