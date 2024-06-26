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

@Component
@RequiredArgsConstructor
public class JwtExceptionfilter extends OncePerRequestFilter{
	private final ObjectMapper mapper;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			filterChain.doFilter(request, response);
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
