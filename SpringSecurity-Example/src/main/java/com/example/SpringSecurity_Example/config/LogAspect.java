package com.example.SpringSecurity_Example.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Aspect
public class LogAspect {
	@Around("execution(* com.example.SpringSecurity_Example.controller.apiController.api*(..))")
	public Object logAll(ProceedingJoinPoint point) throws Throwable {
		log.info("User call api(execution)");
		return point.proceed();
	}
}
