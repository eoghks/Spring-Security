package com.example.library.security;

import com.example.library.authz.cache.AuthzCache;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 인증 주체가 보유한 액션 코드를 구한다.
 * 사용자는 역할 캐시, API Key 는 키에 부여된 액션 중 발급자 역할이 지금도 가진 것만(교집합) — 발급자가 강등되면 키도 함께 줄어든다.
 */
@Component
@RequiredArgsConstructor
public class GrantedActionResolver {

	private final AuthzCache authzCache;

	public Set<String> grantedActions(LibraryPrincipal principal) {
		return switch (principal) {
			case UserPrincipal user -> authzCache.roleActionCodes(user.roleId());
			case ApiKeyPrincipal apiKey -> withinOwner(apiKey);
		};
	}

	private Set<String> withinOwner(ApiKeyPrincipal apiKey) {
		Set<String> ownerActions = authzCache.roleActionCodes(apiKey.ownerRoleId());
		return apiKey.actionCodes().stream()
				.filter(ownerActions::contains)
				.collect(Collectors.toUnmodifiableSet());
	}
}
