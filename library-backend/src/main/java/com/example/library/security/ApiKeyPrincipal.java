package com.example.library.security;

import java.util.Set;

/**
 * API Key 로 인증된 호출자. 보유 액션은 요청마다 캐시에서 읽은 api_key_actions 이다.
 * 키의 권한은 발급자의 현재 권한을 넘지 않는다 — 실제 판정은 부여 액션과 발급자 역할 액션의 교집합으로 한다.
 *
 * @param apiKeyId    API Key ID
 * @param name        API Key 이름
 * @param actionCodes 부여된 액션 코드 집합
 * @param allowedIps  허용 IP·CIDR 목록(비면 전체 허용)
 * @param ownerUserId 발급자 사용자 ID(발급자 접속 조건 판정용)
 * @param ownerRoleId 발급자의 현재 역할 ID(요청마다 캐시에서 읽은 값)
 */
public record ApiKeyPrincipal(Long apiKeyId, String name, Set<String> actionCodes, Set<String> allowedIps,
		Long ownerUserId, Long ownerRoleId) implements LibraryPrincipal {

	public ApiKeyPrincipal {
		actionCodes = Set.copyOf(actionCodes);
		allowedIps = Set.copyOf(allowedIps);
	}
}
