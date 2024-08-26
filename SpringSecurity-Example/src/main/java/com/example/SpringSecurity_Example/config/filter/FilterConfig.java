package com.example.SpringSecurity_Example.config.filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.Filter;

@Configuration
public class FilterConfig {
	@Bean
	FilterRegistrationBean<Filter> secondFilterRegister() {
		FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<Filter>(new SecondFilter());
		registrationBean.setOrder(2);
		return registrationBean;
	}

	@Bean
	FilterRegistrationBean<Filter> firstFilterRegister() {
		FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<Filter>(new FirstFilter());
		registrationBean.setOrder(1);
		registrationBean.addUrlPatterns("/login");
		return registrationBean;
	}
}
