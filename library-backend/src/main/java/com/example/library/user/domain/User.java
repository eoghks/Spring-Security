package com.example.library.user.domain;

import com.example.library.authz.domain.Role;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원. 역할은 1개만 가진다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String username;

	private String password;

	private String name;

	private String email;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "role_id")
	private Role role;

	private int failedLoginCount;

	private boolean locked;

	private LocalDateTime lockedAt;

	private LocalDateTime createdAt;

	@Builder
	private User(String username, String password, String name, String email, Role role, LocalDateTime createdAt) {
		this.username = username;
		this.password = password;
		this.name = name;
		this.email = email;
		this.role = role;
		this.createdAt = createdAt;
	}

	/**
	 * 로그인 실패를 기록하고, 최대 횟수에 도달하면 계정을 잠근다.
	 *
	 * @return 이번 실패로 잠겼으면 true
	 */
	public boolean recordLoginFailure(int maxAttempts, LocalDateTime now) {
		failedLoginCount++;
		if (!locked && failedLoginCount >= maxAttempts) {
			locked = true;
			lockedAt = now;
			return true;
		}
		return false;
	}

	/** 로그인 성공 시 실패 횟수를 초기화한다 */
	public void resetLoginFailures() {
		failedLoginCount = 0;
	}

	/** 관리자 잠금 해제 */
	public void unlock() {
		locked = false;
		lockedAt = null;
		failedLoginCount = 0;
	}

	public void changeRole(Role newRole) {
		this.role = newRole;
	}

	public Long getRoleId() {
		return role.getId();
	}
}
