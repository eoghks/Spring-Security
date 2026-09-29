package com.example.library.security;

import org.springframework.security.access.AccessDeniedException;

/**
 * 접속 조건(IP·기간·요일·시간) 위반. 구체 사유는 로그에만 남기고 응답에는 코드만 준다.
 */
public class AccessConditionDeniedException extends AccessDeniedException {

	public AccessConditionDeniedException(String reason) {
		super(reason);
	}
}
