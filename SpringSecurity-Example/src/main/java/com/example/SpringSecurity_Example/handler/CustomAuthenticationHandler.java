package com.example.SpringSecurity_Example.handler;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.example.SpringSecurity_Example.model.error.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationHandler implements AuthenticationEntryPoint{
	private final ObjectMapper mapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
		String msg = "로그인이 필요합니다.";
		log.info(msg, authException);

		ApiError apiError = new ApiError();
		apiError.getMsg().add(msg);
		apiError.setStatus(HttpStatus.UNAUTHORIZED.value());
		apiError.setHttpStatus(HttpStatus.UNAUTHORIZED);
		String result = mapper.writeValueAsString(apiError);

		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(result);
	}
}
