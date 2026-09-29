package com.example.library.auth.application;

import com.example.library.access.domain.AccessConditionEvaluator;
import com.example.library.auth.api.LoginRequest;
import com.example.library.auth.api.TokenResponse;
import com.example.library.auth.token.JwtProvider;
import com.example.library.authz.cache.AuthzCache;
import com.example.library.authz.cache.UserAuthSnapshot;
import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import com.example.library.common.net.IpPatterns;
import com.example.library.security.LoginFailureLimiter;
import com.example.library.user.domain.User;
import com.example.library.user.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인·토큰 재발급·로그아웃.
 * 비밀번호가 맞아도 사용자 접속 조건(IP·기간·요일·시간)을 벗어나면 토큰을 발급하지 않는다(재발급도 동일).
 * 계정 잠금 여부는 비밀번호가 맞은 뒤에만 알려 준다 — 틀린 비밀번호에는 잠금 여부와 무관하게 같은 INVALID_CREDENTIALS 다.
 * 로그인 실패는 계정별 잠금과 별개로 클라이언트 IP 별로도 세어, 한도를 넘긴 IP 는 그 분 동안 429 로 거절한다.
 */
@Slf4j
@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final LoginAttemptService loginAttemptService;
	private final RefreshTokenService refreshTokenService;
	private final JwtProvider jwtProvider;
	private final AuthzCache authzCache;
	private final LoginFailureLimiter loginFailureLimiter;
	private final Clock clock;

	/** 존재하지 않는 아이디도 같은 시간만큼 해시 비교를 해 계정 존재 여부가 응답 시간으로 드러나지 않게 한다 */
	private final String dummyHash;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			LoginAttemptService loginAttemptService, RefreshTokenService refreshTokenService,
			JwtProvider jwtProvider, AuthzCache authzCache, LoginFailureLimiter loginFailureLimiter, Clock clock) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.loginAttemptService = loginAttemptService;
		this.refreshTokenService = refreshTokenService;
		this.jwtProvider = jwtProvider;
		this.authzCache = authzCache;
		this.loginFailureLimiter = loginFailureLimiter;
		this.clock = clock;
		this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
	}

	/**
	 * 로그인. 접속 조건 위반은 비밀번호 확인 뒤에 검사하며 로그인 실패 횟수에 넣지 않는다.
	 */
	public TokenResponse login(LoginRequest request, String clientIp) {
		if (loginFailureLimiter.isBlocked(clientIp)) {
			throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
		}
		User user = userRepository.findByUsername(request.username())
				.orElseThrow(() -> invalidCredentialsAfterDummyCheck(request.password(), clientIp));
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			loginAttemptService.recordFailure(user.getId());
			loginFailureLimiter.recordFailure(clientIp);
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
		}
		if (user.isLocked()) {
			throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
		}
		UserAuthSnapshot snapshot = authzCache.findUser(user.getId())
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
		ensureAccessAllowed(snapshot, clientIp);
		loginAttemptService.recordSuccess(user.getId());
		return issueTokens(user.getId());
	}

	/**
	 * Refresh 토큰 회전: 기존 토큰을 폐기하고 새 Access·Refresh 토큰을 발급한다.
	 * 접속 조건 위반이면 제시된 토큰은 폐기된 채로 두고 403 을 준다(조건 밖에서는 세션을 이어가지 않는다).
	 */
	@Transactional(noRollbackFor = BusinessException.class)
	public TokenResponse refresh(String refreshToken, String clientIp) {
		Long userId = refreshTokenService.consume(refreshToken);
		UserAuthSnapshot user = authzCache.findUser(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
		if (user.locked()) {
			throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
		}
		ensureAccessAllowed(user, clientIp);
		return issueTokens(userId);
	}

	public void logout(String refreshToken) {
		refreshTokenService.revoke(refreshToken);
	}

	/** 인증 필터와 같은 판정기로 접속 조건을 검사한다. 구체 사유는 로그에만 남긴다 */
	private void ensureAccessAllowed(UserAuthSnapshot user, String clientIp) {
		AccessConditionEvaluator.findViolation(user.accessCondition(), clientIp, LocalDateTime.now(clock))
				.ifPresent(reason -> {
					log.warn("접속 조건 위반으로 토큰 발급 거부: principal=user:{}, ip={}, 사유={}", user.userId(),
							IpPatterns.mask(clientIp), reason);
					throw new BusinessException(ErrorCode.ACCESS_CONDITION_DENIED);
				});
	}

	private TokenResponse issueTokens(Long userId) {
		String accessToken = jwtProvider.createAccessToken(userId);
		String refreshToken = refreshTokenService.issue(userId);
		return TokenResponse.bearer(accessToken, refreshToken, jwtProvider.accessTokenTtlSeconds());
	}

	private BusinessException invalidCredentialsAfterDummyCheck(String rawPassword, String clientIp) {
		passwordEncoder.matches(rawPassword, dummyHash);
		loginFailureLimiter.recordFailure(clientIp);
		return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
	}
}
