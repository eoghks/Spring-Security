package com.example.library.auth.application;

import com.example.library.auth.domain.RefreshTokenRepository;
import com.example.library.authz.application.GrantGuard;
import com.example.library.authz.cache.AuthzChangedEvent;
import com.example.library.config.AppSecurityProperties;
import com.example.library.user.domain.User;
import com.example.library.user.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 성공·실패 기록. 로그인 자체가 실패(예외)해도 실패 횟수는 커밋되도록 별도 트랜잭션으로 둔다.
 * 실패 횟수 증가와 잠금 판정은 모두 DB 원자 연산(UPDATE)으로 해, 동시 실패 요청이 횟수를 유실하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final GrantGuard grantGuard;
	private final AppSecurityProperties securityProperties;
	private final ApplicationEventPublisher eventPublisher;
	private final Clock clock;

	/**
	 * 실패를 기록하고, 한도에 도달했으면 잠근다. 이미 잠긴 계정도 횟수는 계속 올린다.
	 * 잠그는 순간 살아 있는 Refresh 토큰도 모두 폐기한다(잠금 해제 뒤에도 옛 세션이 되살아나지 않게).
	 * 마지막 활성 관리자는 잠그지 않는다(잠그면 해제할 사람이 없다) — 이 계정은 IP 단위 로그인 제한만 적용된다.
	 */
	@Transactional
	public void recordFailure(Long userId) {
		userRepository.incrementFailedLoginCount(userId);
		int maxAttempts = securityProperties.maxLoginAttempts();
		if (isProtectedLastAdmin(userId, maxAttempts)) {
			return;
		}
		LocalDateTime now = LocalDateTime.now(clock);
		// 증가된 DB 값으로 판정하고, 조건부 UPDATE 라 동시에 한도에 닿아도 잠금 처리는 한 요청만 한다
		if (userRepository.lockIfLimitReached(userId, maxAttempts, now) == 1) {
			log.warn("로그인 {}회 실패로 계정 잠금: userId={}", maxAttempts, userId);
			refreshTokenRepository.revokeAllByUserId(userId, now);
			eventPublisher.publishEvent(new AuthzChangedEvent.UserChanged(userId));
		}
	}

	/** 한도에 도달한 계정이 마지막 활성 관리자면 true(잠금 대상에서 뺀다) */
	private boolean isProtectedLastAdmin(Long userId, int maxAttempts) {
		User user = userRepository.findWithRoleById(userId).orElseThrow();
		if (user.isLocked() || user.getFailedLoginCount() < maxAttempts) {
			return false;
		}
		boolean lastAdmin = grantGuard.isLastActiveAdmin(user.getRole(), userId);
		if (lastAdmin) {
			log.warn("마지막 활성 관리자라 로그인 실패 한도에 도달해도 잠그지 않음: userId={}", userId);
		}
		return lastAdmin;
	}

	@Transactional
	public void recordSuccess(Long userId) {
		userRepository.resetFailedLoginCount(userId);
	}
}
