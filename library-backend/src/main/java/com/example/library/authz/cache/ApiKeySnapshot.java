package com.example.library.authz.cache;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 캐시에 담는 API Key 인증 정보.
 *
 * @param apiKeyId    API Key ID
 * @param name        이름
 * @param revoked     폐기 여부
 * @param expiresAt   만료 시각(만료 없음이면 LocalDateTime.MAX)
 * @param actionCodes 부여 액션 코드
 * @param allowedIps  허용 IP·CIDR(비면 전체 허용)
 */
public record ApiKeySnapshot(Long apiKeyId, String name, boolean revoked, LocalDateTime expiresAt,
		Set<String> actionCodes, Set<String> allowedIps) implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	public ApiKeySnapshot {
		actionCodes = Set.copyOf(actionCodes);
		allowedIps = Set.copyOf(allowedIps);
	}

	/** 폐기되지 않았고 만료 전이면 사용 가능 */
	public boolean isUsable(LocalDateTime now) {
		return !revoked && now.isBefore(expiresAt);
	}
}
