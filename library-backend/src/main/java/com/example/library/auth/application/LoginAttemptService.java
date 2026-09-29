package com.example.library.auth.application;

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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

	private final UserRepository userRepository;
	private final AppSecurityProperties securityProperties;
	private final ApplicationEventPublisher eventPublisher;
	private final Clock clock;

	/**
	 * 실패를 기록한다.
	 *
	 * @return 기록 후 계정이 잠긴 상태면 true
	 */
	@Transactional
	public boolean recordFailure(Long userId) {
		User user = userRepository.findById(userId).orElseThrow();
		boolean lockedNow = user.recordLoginFailure(securityProperties.maxLoginAttempts(), LocalDateTime.now(clock));
		if (lockedNow) {
			log.warn("로그인 {}회 실패로 계정 잠금: userId={}", securityProperties.maxLoginAttempts(), userId);
			eventPublisher.publishEvent(new AuthzChangedEvent.UserChanged(userId));
		}
		return user.isLocked();
	}

	@Transactional
	public void recordSuccess(Long userId) {
		userRepository.findById(userId)
				.filter(user -> user.getFailedLoginCount() > 0)
				.ifPresent(User::resetLoginFailures);
	}
}
