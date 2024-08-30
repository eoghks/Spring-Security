package com.example.SpringSecurity_Example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.SpringSecurity_Example.config.interceptor.FirstInterceptor;
import com.example.SpringSecurity_Example.config.interceptor.SecondInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer{
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new FirstInterceptor())
		.order(1)
		.addPathPatterns("/**") //하위 전부에 적용
		.excludePathPatterns("/css/**","/*.ico","/error");

		registry.addInterceptor(new SecondInterceptor())
		.order(2)
		.addPathPatterns("/api*") //api로 시작하는 path에 적용
		.excludePathPatterns("/css/**","/*.ico","/error");
	}
}
