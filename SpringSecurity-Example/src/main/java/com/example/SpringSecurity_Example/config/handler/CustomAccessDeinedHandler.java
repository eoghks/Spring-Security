package com.example.SpringSecurity_Example.config.handler;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.example.SpringSecurity_Example.model.constant.MessageEnum;
import com.example.SpringSecurity_Example.model.vo.ApiErrorResultVo;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomAccessDeinedHandler implements AccessDeniedHandler{
	private final ObjectMapper mapper;

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {
		log.info(MessageEnum.NotExistAccessPriv.getLogMsg(), accessDeniedException);

		ApiErrorResultVo apiError = new ApiErrorResultVo();
		apiError.getMsgs().add(MessageEnum.NotExistAccessPriv.getMsg());
		apiError.setStatus(HttpStatus.FORBIDDEN.value());
		apiError.setHttpStatus(HttpStatus.FORBIDDEN);
		String result = mapper.writeValueAsString(apiError);

		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(result);

	}

}
