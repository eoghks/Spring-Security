package com.example.library.apikey.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * API Key. 원문은 저장하지 않고 SHA-256 해시와 표시용 앞 8자(prefix)만 저장한다.
 * 권한은 사용자 역할과 같은 액션 체계(api_key_actions)로 부여한다.
 */
@Entity
@Table(name = "api_keys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiKey {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	private String keyPrefix;

	private String keyHash;

	private Long ownerUserId;

	@Getter(AccessLevel.NONE)
	private LocalDateTime expiresAt;

	@Getter(AccessLevel.NONE)
	private LocalDateTime revokedAt;

	private LocalDateTime createdAt;

	@Getter(AccessLevel.NONE)
	private LocalDateTime lastUsedAt;

	@ElementCollection
	@CollectionTable(name = "api_key_actions", joinColumns = @JoinColumn(name = "api_key_id"))
	@Column(name = "action_id")
	private Set<Long> actionIds = new HashSet<>();

	@ElementCollection
	@CollectionTable(name = "api_key_allowed_ips", joinColumns = @JoinColumn(name = "api_key_id"))
	@Column(name = "ip")
	private Set<String> allowedIps = new HashSet<>();

	@Builder
	private ApiKey(String name, String keyPrefix, String keyHash, Long ownerUserId, LocalDateTime expiresAt,
			LocalDateTime createdAt, Set<Long> actionIds, Set<String> allowedIps) {
		this.name = name;
		this.keyPrefix = keyPrefix;
		this.keyHash = keyHash;
		this.ownerUserId = ownerUserId;
		this.expiresAt = expiresAt;
		this.createdAt = createdAt;
		this.actionIds = new HashSet<>(actionIds);
		this.allowedIps = new HashSet<>(allowedIps);
	}

	public Optional<LocalDateTime> getExpiresAt() {
		return Optional.ofNullable(expiresAt);
	}

	public Optional<LocalDateTime> getRevokedAt() {
		return Optional.ofNullable(revokedAt);
	}

	public Optional<LocalDateTime> getLastUsedAt() {
		return Optional.ofNullable(lastUsedAt);
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isExpired(LocalDateTime now) {
		return expiresAt != null && !now.isBefore(expiresAt);
	}

	public void revoke(LocalDateTime now) {
		this.revokedAt = now;
	}
}
