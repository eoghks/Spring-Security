package com.example.library.authz.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 권한 변경 이벤트를 받아 커밋 후 캐시를 evict 한다.
 * 커밋 전에 evict 하면 다른 요청이 옛 값을 다시 적재할 수 있으므로 AFTER_COMMIT 에서만 처리한다.
 */
@Component
@RequiredArgsConstructor
public class AuthzCacheEvictionListener {

	private final AuthzCache authzCache;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onChanged(AuthzChangedEvent event) {
		switch (event) {
			case AuthzChangedEvent.UserChanged changed -> authzCache.evictUser(changed.userId());
			case AuthzChangedEvent.RoleChanged changed -> authzCache.evictRole(changed.roleId());
		}
	}
}
