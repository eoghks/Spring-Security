package com.example.library.security;

import com.example.library.authz.cache.AuthzCache;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 인증 주체가 보유한 액션 코드를 구한다. 사용자는 역할 캐시, API Key 는 키에 부여된 액션.
 */
@Component
@RequiredArgsConstructor
public class GrantedActionResolver {

	private final AuthzCache authzCache;

	public Set<String> grantedActions(LibraryPrincipal principal) {
		return switch (principal) {
			case UserPrincipal user -> authzCache.roleActionCodes(user.roleId());
			case ApiKeyPrincipal apiKey -> apiKey.actionCodes();
		};
	}
}
