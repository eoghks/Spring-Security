package com.example.library.security;

import com.example.library.common.error.ErrorCode;
import com.example.library.common.error.ErrorResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 403 응답을 만드는 유일한 지점. 접속 조건 위반은 ACCESS_CONDITION_DENIED 로 구분한다.
 */
@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

	private final ErrorResponseWriter errorResponseWriter;

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		ErrorCode errorCode = accessDeniedException instanceof AccessConditionDeniedException
				? ErrorCode.ACCESS_CONDITION_DENIED
				: ErrorCode.ACCESS_DENIED;
		errorResponseWriter.write(request, response, errorCode);
	}
}
