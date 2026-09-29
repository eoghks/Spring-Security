package com.example.library.authz.cache;

import java.util.List;

/**
 * Hazelcast 분산 자료구조 이름.
 */
public final class CacheNames {

	/** userId → 사용자 인증 스냅샷(역할·잠금·접속 조건) */
	public static final String USER_AUTH = "user-auth";

	/** roleId → 보유 액션 코드 집합 */
	public static final String ROLE_ACTIONS = "role-actions";

	/** API Key 해시 → API Key 스냅샷(액션·허용 IP·만료) */
	public static final String API_KEYS = "api-keys";

	/** 존재하지 않는 API Key 해시(음성 캐시, 항목별 짧은 TTL) */
	public static final String API_KEY_MISSES = "api-key-misses";

	/** "클라이언트 IP|분" → API Key 인증 실패 횟수(분 단위 버킷) */
	public static final String API_KEY_FAILURES = "api-key-failures";

	/** "클라이언트 IP|분" → 로그인 실패 횟수(분 단위 버킷) */
	public static final String LOGIN_FAILURES = "login-failures";

	/** 인가 규칙(action_urls) 재로딩 알림 토픽 */
	public static final String AUTHZ_RELOAD_TOPIC = "authz-rules-reload";

	/** TTL 을 적용할 IMap 목록 */
	public static final List<String> MAPS = List.of(USER_AUTH, ROLE_ACTIONS, API_KEYS);

	private CacheNames() {
	}
}
