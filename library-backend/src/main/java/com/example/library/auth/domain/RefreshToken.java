package com.example.library.auth.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Refresh 토큰. 원문 대신 SHA-256 해시만 저장한다.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long userId;

	private String tokenHash;

	private LocalDateTime expiresAt;

	private LocalDateTime revokedAt;

	private LocalDateTime createdAt;

	@Builder
	private RefreshToken(Long userId, String tokenHash, LocalDateTime expiresAt, LocalDateTime createdAt) {
		this.userId = userId;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
		this.createdAt = createdAt;
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isExpired(LocalDateTime now) {
		return !now.isBefore(expiresAt);
	}

	public void revoke(LocalDateTime now) {
		if (revokedAt == null) {
			revokedAt = now;
		}
	}
}
