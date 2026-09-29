package com.example.library.security;

import java.util.Set;

/**
 * API Key 로 인증된 호출자. 보유 액션은 요청마다 캐시에서 읽은 api_key_actions 이다.
 *
 * @param apiKeyId    API Key ID
 * @param name        API Key 이름
 * @param actionCodes 부여된 액션 코드 집합
 * @param allowedIps  허용 IP·CIDR 목록(비면 전체 허용)
 */
public record ApiKeyPrincipal(Long apiKeyId, String name, Set<String> actionCodes, Set<String> allowedIps)
		implements LibraryPrincipal {

	public ApiKeyPrincipal {
		actionCodes = Set.copyOf(actionCodes);
		allowedIps = Set.copyOf(allowedIps);
	}
}
