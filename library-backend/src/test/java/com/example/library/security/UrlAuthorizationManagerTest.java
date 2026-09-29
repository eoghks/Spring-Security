package com.example.library.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.library.authz.rule.AuthorizationRuleRegistry;
import com.example.library.authz.rule.UrlRuleSource;
import com.example.library.authz.rule.UrlRuleTable;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

class UrlAuthorizationManagerTest {

	private final AuthorizationRuleRegistry registry = mock(AuthorizationRuleRegistry.class);
	private final GrantedActionResolver resolver = mock(GrantedActionResolver.class);
	private final UrlAuthorizationManager manager = new UrlAuthorizationManager(registry, resolver);

	private final UserPrincipal member = new UserPrincipal(1L, "member", 3L);

	@BeforeEach
	void setUp() {
		given(registry.current()).willReturn(UrlRuleTable.of(List.of(
				UrlRuleSource.forAction("GET", "/api/books", "BOOK:READ"),
				UrlRuleSource.forAction("GET", "/api/books", "BOOK_MANAGE:READ"),
				UrlRuleSource.forAction("DELETE", "/api/books/{id}", "BOOK_MANAGE:DELETE"),
				UrlRuleSource.forAuthenticated("GET", "/api/me"))));
	}

	@Test
	@DisplayName("보유 액션 중 하나라도 규칙의 액션이면 통과한다(OR)")
	void grantedByAnyAction() {
		given(resolver.grantedActions(any())).willReturn(Set.of("BOOK_MANAGE:READ"));

		assertThat(decide(user(member), "GET", "/api/books")).isTrue();
	}

	@Test
	@DisplayName("규칙의 액션을 하나도 보유하지 않으면 거부한다")
	void deniedWithoutAction() {
		given(resolver.grantedActions(any())).willReturn(Set.of("BOOK:READ"));

		assertThat(decide(user(member), "DELETE", "/api/books/1")).isFalse();
	}

	@Test
	@DisplayName("미등록 URL 은 모든 권한을 가져도 거부한다(fail-closed)")
	void unregisteredDenied() {
		given(resolver.grantedActions(any())).willReturn(Set.of("BOOK:READ", "BOOK_MANAGE:READ"));

		assertThat(decide(user(member), "GET", "/api/secret")).isFalse();
	}

	@Test
	@DisplayName("authenticated_urls 는 로그인 사용자면 통과, API Key 는 거부한다")
	void authenticatedOnly() {
		given(resolver.grantedActions(any())).willReturn(Set.of());
		ApiKeyPrincipal apiKey = new ApiKeyPrincipal(1L, "key", Set.of("BOOK:READ"), Set.of(), 1L, 1L);

		assertThat(decide(user(member), "GET", "/api/me")).isTrue();
		assertThat(decide(user(apiKey), "GET", "/api/me")).isFalse();
	}

	@Test
	@DisplayName("익명 사용자는 거부한다")
	void anonymousDenied() {
		Authentication anonymous = new AnonymousAuthenticationToken("key", "anonymousUser",
				AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

		assertThat(decide(anonymous, "GET", "/api/books")).isFalse();
	}

	private Authentication user(LibraryPrincipal principal) {
		return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
	}

	private boolean decide(Authentication authentication, String method, String uri) {
		MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
		return manager.check(() -> authentication, new RequestAuthorizationContext(request)).isGranted();
	}
}
