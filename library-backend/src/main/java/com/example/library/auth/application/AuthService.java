package com.example.library.auth.application;

import com.example.library.auth.api.LoginRequest;
import com.example.library.auth.api.TokenResponse;
import com.example.library.auth.token.JwtProvider;
import com.example.library.authz.cache.AuthzCache;
import com.example.library.authz.cache.UserAuthSnapshot;
import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import com.example.library.user.domain.User;
import com.example.library.user.domain.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인·토큰 재발급·로그아웃.
 */
@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final LoginAttemptService loginAttemptService;
	private final RefreshTokenService refreshTokenService;
	private final JwtProvider jwtProvider;
	private final AuthzCache authzCache;

	/** 존재하지 않는 아이디도 같은 시간만큼 해시 비교를 해 계정 존재 여부가 응답 시간으로 드러나지 않게 한다 */
	private final String dummyHash;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			LoginAttemptService loginAttemptService, RefreshTokenService refreshTokenService,
			JwtProvider jwtProvider, AuthzCache authzCache) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.loginAttemptService = loginAttemptService;
		this.refreshTokenService = refreshTokenService;
		this.jwtProvider = jwtProvider;
		this.authzCache = authzCache;
		this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
	}

	public TokenResponse login(LoginRequest request) {
		User user = userRepository.findByUsername(request.username())
				.orElseThrow(() -> invalidCredentialsAfterDummyCheck(request.password()));
		if (user.isLocked()) {
			throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
		}
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			boolean locked = loginAttemptService.recordFailure(user.getId());
			throw new BusinessException(locked ? ErrorCode.ACCOUNT_LOCKED : ErrorCode.INVALID_CREDENTIALS);
		}
		loginAttemptService.recordSuccess(user.getId());
		return issueTokens(user.getId());
	}

	/** Refresh 토큰 회전: 기존 토큰을 폐기하고 새 Access·Refresh 토큰을 발급한다 */
	@Transactional(noRollbackFor = BusinessException.class)
	public TokenResponse refresh(String refreshToken) {
		Long userId = refreshTokenService.consume(refreshToken);
		UserAuthSnapshot user = authzCache.findUser(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
		if (user.locked()) {
			throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
		}
		return issueTokens(userId);
	}

	public void logout(String refreshToken) {
		refreshTokenService.revoke(refreshToken);
	}

	private TokenResponse issueTokens(Long userId) {
		String accessToken = jwtProvider.createAccessToken(userId);
		String refreshToken = refreshTokenService.issue(userId);
		return TokenResponse.bearer(accessToken, refreshToken, jwtProvider.accessTokenTtlSeconds());
	}

	private BusinessException invalidCredentialsAfterDummyCheck(String rawPassword) {
		passwordEncoder.matches(rawPassword, dummyHash);
		return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
	}
}
