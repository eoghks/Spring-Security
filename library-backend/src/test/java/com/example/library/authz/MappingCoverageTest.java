package com.example.library.authz;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.authz.domain.ActionUrlRepository;
import com.example.library.authz.domain.AuthenticatedUrlRepository;
import com.example.library.security.PublicEndpoints;
import com.example.library.support.IntegrationTestSupport;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.server.PathContainer;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 모든 컨트롤러 매핑이 인가 규칙(action_urls / authenticated_urls / permitAll) 중 하나에 등록돼 있는지 대조한다.
 * 규칙이 없는 URL 은 fail-closed 로 항상 403 이 되므로, 새 API 를 만들고 등록을 잊으면 이 테스트가 빌드를 깨뜨린다.
 * 반대로 규칙은 있는데 컨트롤러가 없는(오타·삭제된) URL 도 찾아낸다.
 */
class MappingCoverageTest extends IntegrationTestSupport {

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	private RequestMappingHandlerMapping handlerMapping;

	@Autowired
	private ActionUrlRepository actionUrlRepository;

	@Autowired
	private AuthenticatedUrlRepository authenticatedUrlRepository;

	@Test
	@DisplayName("모든 애플리케이션 컨트롤러 매핑은 인가 규칙에 등록돼 있다")
	void everyMappingIsRegistered() {
		Set<String> registered = registeredRules();

		List<String> missing = applicationMappings().stream()
				.filter(key -> !registered.contains(key) && !isPermitAll(key))
				.toList();

		assertThat(missing).as("action_urls·authenticated_urls·PublicEndpoints 어디에도 없는 매핑").isEmpty();
	}

	@Test
	@DisplayName("등록된 인가 규칙은 모두 실제 컨트롤러 매핑을 가리킨다")
	void everyRuleHasMapping() {
		Set<String> mappings = applicationMappings();

		List<String> dangling = registeredRules().stream().filter(key -> !mappings.contains(key)).toList();

		assertThat(dangling).as("컨트롤러가 없는 인가 규칙(오타 가능성)").isEmpty();
	}

	/** "METHOD /pattern" 형식의 애플리케이션 매핑 집합(스프링·springdoc 내부 컨트롤러 제외) */
	private Set<String> applicationMappings() {
		return handlerMapping.getHandlerMethods().entrySet().stream()
				.filter(entry -> isApplicationHandler(entry.getValue()))
				.flatMap(entry -> keysOf(entry.getKey()))
				.collect(Collectors.toCollection(TreeSet::new));
	}

	private boolean isApplicationHandler(HandlerMethod handler) {
		return handler.getBeanType().getPackageName().startsWith("com.example.library");
	}

	private Stream<String> keysOf(RequestMappingInfo info) {
		Set<String> patterns = info.getPathPatternsCondition().getPatternValues();
		Set<String> methods = info.getMethodsCondition().getMethods().stream()
				.map(Enum::name).collect(Collectors.toSet());
		// 메서드 제한 없는 매핑은 모든 메서드로 열려 있으므로 규칙과 일치할 수 없다 — "ANY" 로 드러낸다
		Set<String> effectiveMethods = methods.isEmpty() ? Set.of("ANY") : methods;
		return patterns.stream().flatMap(pattern -> effectiveMethods.stream().map(method -> method + " " + pattern));
	}

	private Set<String> registeredRules() {
		Set<String> rules = new TreeSet<>();
		actionUrlRepository.findAll().forEach(url -> rules.add(url.getHttpMethod() + " " + url.getUrlPattern()));
		authenticatedUrlRepository.findAll().forEach(url -> rules.add(url.getHttpMethod() + " " + url.getUrlPattern()));
		return rules;
	}

	private boolean isPermitAll(String key) {
		String method = key.substring(0, key.indexOf(' '));
		String pattern = key.substring(key.indexOf(' ') + 1);
		PathPatternParser parser = new PathPatternParser();
		return PublicEndpoints.ALL.stream()
				.filter(endpoint -> endpoint.method().map(m -> m.name().equals(method)).orElse(true))
				.anyMatch(endpoint -> endpoint.pattern().equals(pattern)
						|| parser.parse(endpoint.pattern()).matches(PathContainer.parsePath(pattern)));
	}
}
