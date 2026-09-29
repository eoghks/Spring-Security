package com.example.library.apikey.api;

import com.example.library.apikey.domain.ApiKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * API Key 목록·상세 응답(원문 없음, 앞 8자만).
 *
 * @param status ACTIVE / REVOKED / EXPIRED
 */
public record ApiKeyResponse(Long id, String name, String keyPrefix, Long ownerUserId, List<String> actionCodes,
		List<String> allowedIps, LocalDateTime expiresAt, LocalDateTime revokedAt, LocalDateTime createdAt,
		LocalDateTime lastUsedAt, String status) {

	public static ApiKeyResponse of(ApiKey apiKey, Set<String> actionCodes, LocalDateTime now) {
		return new ApiKeyResponse(apiKey.getId(), apiKey.getName(), apiKey.getKeyPrefix(), apiKey.getOwnerUserId(),
				actionCodes.stream().sorted().toList(), apiKey.getAllowedIps().stream().sorted().toList(),
				apiKey.getExpiresAt().orElse(null), apiKey.getRevokedAt().orElse(null), apiKey.getCreatedAt(),
				apiKey.getLastUsedAt().orElse(null), statusOf(apiKey, now));
	}

	private static String statusOf(ApiKey apiKey, LocalDateTime now) {
		if (apiKey.isRevoked()) {
			return "REVOKED";
		}
		return apiKey.isExpired(now) ? "EXPIRED" : "ACTIVE";
	}
}
