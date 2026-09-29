package com.example.library.security;

import com.example.library.access.domain.AccessConditionEvaluator;
import com.example.library.access.domain.AccessConditionSnapshot;
import com.example.library.authz.cache.AuthzCache;
import com.example.library.authz.cache.UserAuthSnapshot;
import com.example.library.common.net.ClientIpResolver;
import com.example.library.common.net.IpPatterns;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 인증된 요청마다 접속 조건을 검사한다.
 * 사용자는 user_access_conditions(IP·기간·요일·시간), API Key 는 api_key_allowed_ips 로 판정한다.
 * 위반 시 403 ACCESS_CONDITION_DENIED 로 응답하고 구체 사유는 로그에만 남긴다.
 */
@Slf4j
@RequiredArgsConstructor
public class AccessConditionFilter extends OncePerRequestFilter {

	private final AuthzCache authzCache;
	private final ClientIpResolver clientIpResolver;
	private final AccessDeniedHandler accessDeniedHandler;
	private final Clock clock;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Optional<LibraryPrincipal> principal = currentPrincipal();
		if (principal.isPresent()) {
			String clientIp = clientIpResolver.resolve(request);
			Optional<String> violation = findViolation(principal.get(), clientIp);
			if (violation.isPresent()) {
				log.warn("접속 조건 위반: principal={}, ip={}, 사유={}", describe(principal.get()),
						IpPatterns.mask(clientIp), violation.get());
				accessDeniedHandler.handle(request, response, new AccessConditionDeniedException(violation.get()));
				return;
			}
		}
		chain.doFilter(request, response);
	}

	private Optional<String> findViolation(LibraryPrincipal principal, String clientIp) {
		return switch (principal) {
			case UserPrincipal user -> findUserViolation(user, clientIp);
			case ApiKeyPrincipal apiKey -> IpPatterns.matchesAny(List.copyOf(apiKey.allowedIps()), clientIp)
					? Optional.empty()
					: Optional.of("API Key 허용 IP 아님");
		};
	}

	/** 스냅샷이 사라졌으면(인증 직후 삭제된 사용자) 통과시키지 않는다 */
	private Optional<String> findUserViolation(UserPrincipal user, String clientIp) {
		Optional<AccessConditionSnapshot> condition = authzCache.findUser(user.userId())
				.map(UserAuthSnapshot::accessCondition);
		if (condition.isEmpty()) {
			return Optional.of("사용자 정보 없음");
		}
		return AccessConditionEvaluator.findViolation(condition.get(), clientIp, LocalDateTime.now(clock));
	}

	private Optional<LibraryPrincipal> currentPrincipal() {
		return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
				.filter(Authentication::isAuthenticated)
				.map(Authentication::getPrincipal)
				.filter(LibraryPrincipal.class::isInstance)
				.map(LibraryPrincipal.class::cast);
	}

	private String describe(LibraryPrincipal principal) {
		return switch (principal) {
			case UserPrincipal user -> "user:" + user.userId();
			case ApiKeyPrincipal apiKey -> "apiKey:" + apiKey.apiKeyId();
		};
	}
}
