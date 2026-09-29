package com.example.library.config;

import com.example.library.apikey.application.ApiKeyUsageRecorder;
import com.example.library.apikey.domain.ApiKeyCodec;
import com.example.library.auth.token.JwtProvider;
import com.example.library.authz.cache.AuthzCache;
import com.example.library.common.error.ErrorResponseWriter;
import com.example.library.common.net.ClientIpResolver;
import com.example.library.security.AccessConditionFilter;
import com.example.library.security.ApiKeyFailureLimiter;
import com.example.library.security.ApiKeyAuthenticationFilter;
import com.example.library.security.JsonAccessDeniedHandler;
import com.example.library.security.JsonAuthenticationEntryPoint;
import com.example.library.security.JwtAuthenticationFilter;
import com.example.library.security.PublicEndpoints;
import com.example.library.security.UrlAuthorizationManager;
import jakarta.servlet.DispatcherType;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 보안 필터 체인 구성.
 * permitAll 은 PublicEndpoints 한 곳에서만 관리하고, 나머지 모든 요청은 UrlAuthorizationManager 가 판정한다.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtProvider jwtProvider;
	private final AuthzCache authzCache;
	private final UrlAuthorizationManager urlAuthorizationManager;
	private final JsonAuthenticationEntryPoint authenticationEntryPoint;
	private final JsonAccessDeniedHandler accessDeniedHandler;
	private final ClientIpResolver clientIpResolver;
	private final ApiKeyCodec apiKeyCodec;
	private final ApiKeyUsageRecorder apiKeyUsageRecorder;
	private final ApiKeyFailureLimiter apiKeyFailureLimiter;
	private final ErrorResponseWriter errorResponseWriter;
	private final Clock clock;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// Bearer 토큰 기반 무상태 API — 쿠키 세션을 쓰지 않으므로 CSRF 보호가 필요 없다
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.authorizeHttpRequests(authorize -> authorize
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers(PublicEndpoints.requestMatchers()).permitAll()
						.anyRequest().access(urlAuthorizationManager))
				// 순서: API Key 인증 → JWT 인증 → 접속 조건 검사 → (인가)
				// 필터는 빈으로 등록하지 않아 서블릿 필터로 중복 등록되지 않는다
				.addFilterBefore(new ApiKeyAuthenticationFilter(apiKeyCodec, authzCache, apiKeyUsageRecorder,
						authenticationEntryPoint, apiKeyFailureLimiter, clientIpResolver, errorResponseWriter, clock),
						UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(new JwtAuthenticationFilter(jwtProvider, authzCache), ApiKeyAuthenticationFilter.class)
				.addFilterAfter(new AccessConditionFilter(authzCache, clientIpResolver, accessDeniedHandler, clock),
						JwtAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
