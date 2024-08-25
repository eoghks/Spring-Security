package com.example.SpringSecurity_Example.config.filter;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Order(1)
public class SecondFilter implements Filter{
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		log.info("==========두번째 필터 시작!(Bean)==========");
		chain.doFilter(request, response);
		log.info("==========두번째 필터 종료!(Bean)==========");
	}
}