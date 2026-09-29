package com.example.library.auth.api;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 재발급·로그아웃 요청.
 */
public record RefreshTokenRequest(@NotBlank(message = "리프레시 토큰이 필요합니다.") String refreshToken) {

	@Override
	public String toString() {
		return "RefreshTokenRequest[refreshToken=****]";
	}
}
