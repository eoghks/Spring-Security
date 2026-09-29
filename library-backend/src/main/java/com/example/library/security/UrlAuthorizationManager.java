package com.example.library.security;

import com.example.library.authz.rule.AuthorizationRuleRegistry;
import com.example.library.authz.rule.UrlRule;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

/**
 * URL 기반 인가 판정기. permitAll 목록을 제외한 모든 요청이 여기를 지난다.
 * <ol>
 *   <li>인증 주체가 없으면 거부(→ 401)</li>
 *   <li>요청과 일치하는 가장 구체적인 규칙을 찾고, 없으면 거부(미등록 URL — fail-closed, → 403)</li>
 *   <li>authenticated_urls 규칙이면 사용자 로그인만으로 통과</li>
 *   <li>그 외에는 규칙의 액션 중 하나라도 보유하면 통과(OR)</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UrlAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

	private static final AuthorizationDecision DENY = new AuthorizationDecision(false);

	private final AuthorizationRuleRegistry registry;
	private final GrantedActionResolver grantedActionResolver;

	@Override
	public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
		Optional<LibraryPrincipal> principal = principalOf(authentication.get());
		if (principal.isEmpty()) {
			return DENY;
		}
		HttpServletRequest request = context.getRequest();
		String path = pathWithinApplication(request);
		Optional<UrlRule> rule = registry.current().match(request.getMethod(), path);
		if (rule.isEmpty()) {
			log.info("미등록 URL 거부: {} {}", request.getMethod(), path);
			return DENY;
		}
		return new AuthorizationDecision(isGranted(rule.get(), principal.get()));
	}

	private boolean isGranted(UrlRule rule, LibraryPrincipal principal) {
		if (rule.authenticatedOnly() && principal instanceof UserPrincipal) {
			return true;
		}
		return rule.grantsAny(grantedActionResolver.grantedActions(principal));
	}

	private Optional<LibraryPrincipal> principalOf(Authentication authentication) {
		return Optional.ofNullable(authentication)
				.filter(Authentication::isAuthenticated)
				.map(Authentication::getPrincipal)
				.filter(LibraryPrincipal.class::isInstance)
				.map(LibraryPrincipal.class::cast);
	}

	private String pathWithinApplication(HttpServletRequest request) {
		return request.getRequestURI().substring(request.getContextPath().length());
	}
}
