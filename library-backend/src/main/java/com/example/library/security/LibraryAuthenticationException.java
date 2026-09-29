package com.example.library.security;

import com.example.library.common.error.ErrorCode;
import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

/**
 * 오류 코드를 담은 인증 실패. AuthenticationEntryPoint 가 코드별 401 응답을 만든다.
 */
@Getter
public class LibraryAuthenticationException extends AuthenticationException {

	private final ErrorCode errorCode;

	public LibraryAuthenticationException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}
