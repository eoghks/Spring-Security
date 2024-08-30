package com.example.SpringSecurity_Example.config.jwt;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.SpringSecurity_Example.model.vo.ApiErrorResultVo;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtExceptionfilter extends OncePerRequestFilter{
	private final ObjectMapper mapper;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			log.info("==========JWT 필터 시작!(Bean)==========");
			filterChain.doFilter(request, response);
			log.info("==========JWT 필터 종료!(Bean)==========");
		} catch(JwtException e) {
			setErrorResponse(request, response, e);
		}
	}

	private void setErrorResponse(HttpServletRequest request, HttpServletResponse response,
			JwtException e) throws IOException, ServletException {

		ApiErrorResultVo apiError = new ApiErrorResultVo();
		apiError.getMsgs().add(e.getMessage());
		apiError.setStatus(HttpStatus.BAD_REQUEST.value());
		apiError.setHttpStatus(HttpStatus.BAD_REQUEST);
		String result = mapper.writeValueAsString(apiError);

		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setStatus(HttpStatus.BAD_REQUEST.value());
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(result);
	}
}
