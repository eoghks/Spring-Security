package com.example.SpringSecurity_Example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.SpringSecurity_Example.config.handler.CustomAccessDeinedHandler;
import com.example.SpringSecurity_Example.config.handler.CustomAuthenticationHandler;
import com.example.SpringSecurity_Example.config.jwt.JwtAuthFilter;
import com.example.SpringSecurity_Example.config.jwt.JwtExceptionfilter;
import com.example.SpringSecurity_Example.config.jwt.JwtUtil;
import com.example.SpringSecurity_Example.service.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	private final JwtExceptionfilter jwtExceptionfilter;
	private final CustomUserDetailsService customUserDetailsService;
	private final JwtUtil jwtUtil;
	//인증이 안되 경우
	private final CustomAuthenticationHandler authenticationHandler;
	//권한이 없는 경우
	private final CustomAccessDeinedHandler accessDeinedHandler;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
		return http
				//REST API 에서는 basic과  csrf보안을 사용하지 않는다.
				.httpBasic(AbstractHttpConfigurer::disable)
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session
					.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				)
				//인증 인가
				.authorizeHttpRequests((authorize) -> authorize
					.requestMatchers("/api3/**").authenticated()
					.requestMatchers("/api2/**").hasRole("admin")
					.anyRequest().permitAll()
				)
				//jwt
				.addFilterBefore(new JwtAuthFilter(customUserDetailsService, jwtUtil), UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(jwtExceptionfilter, JwtAuthFilter.class)
				//인증 및 인가 실패에 따른 Handler 작성
				.exceptionHandling((exceptionHandling) -> exceptionHandling
						.authenticationEntryPoint(authenticationHandler)
						.accessDeniedHandler(accessDeinedHandler))
				.build();
	}

	//비밀번호 암호화
	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
