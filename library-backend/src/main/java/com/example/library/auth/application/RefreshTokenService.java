package com.example.library.auth.application;

import com.example.library.auth.domain.RefreshToken;
import com.example.library.auth.domain.RefreshTokenRepository;
import com.example.library.auth.token.JwtProperties;
import com.example.library.common.crypto.Hashing;
import com.example.library.common.crypto.SecureTokenGenerator;
import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh 토큰 발급·회전·폐기.
 * 원문은 발급 시 클라이언트에만 주고 DB 에는 해시만 저장한다.
 * 이미 폐기된 토큰이 다시 제시되면 탈취로 보고 해당 사용자의 토큰을 모두 폐기한다(재사용 탐지).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final SecureTokenGenerator tokenGenerator;
	private final JwtProperties jwtProperties;
	private final Clock clock;

	/** 새 Refresh 토큰을 발급하고 원문을 돌려준다 */
	@Transactional
	public String issue(Long userId) {
		String raw = tokenGenerator.generate();
		LocalDateTime now = LocalDateTime.now(clock);
		refreshTokenRepository.save(RefreshToken.builder()
				.userId(userId)
				.tokenHash(Hashing.sha256Hex(raw))
				.expiresAt(now.plus(jwtProperties.refreshTokenTtl()))
				.createdAt(now)
				.build());
		return raw;
	}

	/**
	 * 토큰을 소비(폐기)하고 소유 사용자 ID 를 돌려준다. 회전 시 호출한다.
	 * 재사용 탐지 시 전체 폐기가 롤백되지 않도록 업무 예외에는 롤백하지 않는다.
	 */
	@Transactional(noRollbackFor = BusinessException.class)
	public Long consume(String raw) {
		RefreshToken token = refreshTokenRepository.findByTokenHash(Hashing.sha256Hex(raw))
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
		LocalDateTime now = LocalDateTime.now(clock);
		if (token.isRevoked()) {
			log.warn("폐기된 Refresh 토큰 재사용 탐지 — 사용자 토큰 전체 폐기: userId={}", token.getUserId());
			refreshTokenRepository.revokeAllByUserId(token.getUserId(), now);
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		if (token.isExpired(now)) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		token.revoke(now);
		return token.getUserId();
	}

	/** 로그아웃: 제시된 토큰을 폐기한다(없거나 이미 폐기돼도 성공으로 본다) */
	@Transactional
	public void revoke(String raw) {
		refreshTokenRepository.findByTokenHash(Hashing.sha256Hex(raw))
				.ifPresent(token -> token.revoke(LocalDateTime.now(clock)));
	}
}
