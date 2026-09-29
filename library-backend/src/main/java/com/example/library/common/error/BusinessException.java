package com.example.library.common.error;

import lombok.Getter;

/**
 * 업무 규칙 위반을 나타내는 예외. 전역 핸들러가 ErrorCode 에 맞는 응답으로 변환한다.
 */
@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}
