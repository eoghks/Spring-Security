package com.example.SpringSecurity_Example;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@SpringBootApplication
public class SpringSecurityExampleApplication implements ApplicationContextAware {
	@Autowired
	private static ApplicationContext context;

	public static void main(String[] args) {
		SpringApplication.run(SpringSecurityExampleApplication.class, args);
		//printBean();
	}

	public static void printBean()  {
		for(String bean: context.getBeanDefinitionNames()) {
			System.out.println("SpringBoot Bean: "+ bean);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context = applicationContext;
	}
}
