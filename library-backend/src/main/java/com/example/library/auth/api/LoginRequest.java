package com.example.library.auth.api;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청.
 */
public record LoginRequest(
		@NotBlank(message = "아이디를 입력하세요.") String username,
		@NotBlank(message = "비밀번호를 입력하세요.") String password) {

	/** 로그 등에 비밀번호가 찍히지 않도록 toString 을 가린다 */
	@Override
	public String toString() {
		return "LoginRequest[username=" + username + ", password=****]";
	}
}
