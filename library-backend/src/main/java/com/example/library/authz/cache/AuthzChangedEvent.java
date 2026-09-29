package com.example.library.authz.cache;

/**
 * 권한 캐시 무효화가 필요한 변경. 트랜잭션 커밋 후 해당 키만 evict 한다.
 */
public sealed interface AuthzChangedEvent {

	/** 사용자 역할·잠금·접속 조건 변경 */
	record UserChanged(Long userId) implements AuthzChangedEvent {
	}

	/** 역할 보유 액션 변경 */
	record RoleChanged(Long roleId) implements AuthzChangedEvent {
	}
}
