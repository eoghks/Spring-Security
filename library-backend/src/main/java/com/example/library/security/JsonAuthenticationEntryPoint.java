package com.example.library.security;

import com.example.library.common.error.ErrorCode;
import com.example.library.common.error.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 401 응답을 만드는 유일한 지점.
 * 오류 코드 우선순위: 예외에 담긴 코드 → 인증 필터가 남긴 요청 속성(만료·무효 토큰 등) → UNAUTHORIZED.
 */
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

	/** 인증 필터가 실패 사유를 남기는 요청 속성 이름 */
	public static final String ERROR_CODE_ATTRIBUTE = JsonAuthenticationEntryPoint.class.getName() + ".errorCode";

	private final ErrorResponseWriter errorResponseWriter;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		errorResponseWriter.write(request, response, resolveErrorCode(request, authException));
	}

	private ErrorCode resolveErrorCode(HttpServletRequest request, AuthenticationException exception) {
		if (exception instanceof LibraryAuthenticationException libraryException) {
			return libraryException.getErrorCode();
		}
		return Optional.ofNullable(request.getAttribute(ERROR_CODE_ATTRIBUTE))
				.filter(ErrorCode.class::isInstance)
				.map(ErrorCode.class::cast)
				.orElse(ErrorCode.UNAUTHORIZED);
	}
}
