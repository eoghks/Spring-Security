package com.example.SpringSecurity_Example.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Aspect
public class AspectConfig {
	@Around("execution(* com.example.SpringSecurity_Example.controller.apiController.*(..))")
	public Object logAll(ProceedingJoinPoint point) throws Throwable {
		log.info("aop(execution) 시작!");
		Object obj = point.proceed();
		log.info("aop(execution) 종료!");
		return obj;
	}

	@Around("@annotation(PerLogging)")
	public Object logApi(ProceedingJoinPoint point) throws Throwable {
		log.info("aop(annotation)");
		return point.proceed();
	}
}
