package com.example.library.security;

import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * 인증 없이 호출 가능한 URL(permitAll) 목록 — 이 목록 한 곳에서만 관리한다.
 * 매핑 누락 대조 테스트도 이 목록을 참조한다.
 */
public final class PublicEndpoints {

	/**
	 * permitAll 항목.
	 *
	 * @param method  HTTP 메서드(비면 모든 메서드)
	 * @param pattern URL 패턴
	 */
	public record Endpoint(Optional<HttpMethod> method, String pattern) {

		static Endpoint of(HttpMethod method, String pattern) {
			return new Endpoint(Optional.of(method), pattern);
		}

		static Endpoint anyMethod(String pattern) {
			return new Endpoint(Optional.empty(), pattern);
		}

		RequestMatcher toRequestMatcher() {
			PathPatternRequestMatcher.Builder builder = PathPatternRequestMatcher.withDefaults();
			return method.map(m -> builder.matcher(m, pattern)).orElseGet(() -> builder.matcher(pattern));
		}
	}

	/** Swagger 항목은 springdoc 이 켜진 기본 개발 프로필에서만 실제로 응답한다(그 밖 프로필은 문서가 꺼져 404) */
	public static final List<Endpoint> ALL = List.of(
			Endpoint.of(HttpMethod.POST, "/api/auth/login"),
			Endpoint.of(HttpMethod.POST, "/api/auth/signup"),
			Endpoint.of(HttpMethod.POST, "/api/auth/refresh"),
			Endpoint.of(HttpMethod.POST, "/api/auth/logout"),
			Endpoint.anyMethod("/swagger-ui.html"),
			Endpoint.anyMethod("/swagger-ui/**"),
			Endpoint.anyMethod("/v3/api-docs/**"));

	private PublicEndpoints() {
	}

	public static RequestMatcher[] requestMatchers() {
		return ALL.stream().map(Endpoint::toRequestMatcher).toArray(RequestMatcher[]::new);
	}
}
