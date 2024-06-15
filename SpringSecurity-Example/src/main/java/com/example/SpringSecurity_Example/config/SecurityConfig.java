package com.example.SpringSecurity_Example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	//private JwtTokenPro

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
		return http.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests((authorize) -> authorize
					.requestMatchers("/api3/**").authenticated()
					.requestMatchers("/api2/**").hasRole("admin")
					.anyRequest().permitAll()
				)

				.formLogin(formLogin -> formLogin
					//.loginPage("/login")
					//.loginProcessingUrl("/loginProc")
					.defaultSuccessUrl("/home"))
//				.logout((logout) -> logout
//					.logoutSuccessUrl("/login")
//					.invalidateHttpSession(true))
				.sessionManagement(session -> session
					.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				)
				.build();
	}

//	정적 리소스 제외
//	@Bean
//	public WebSecurityCustomizer webSecurityCustomizer(){
//		return web -> web.ignoring()
//			.requestMatchers(PathRequest
//			.toStaticResources()
//			.atCommonLocations()
//			 );
//	}
//
	//비밀번호 암호화
	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
