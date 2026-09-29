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
import org.hibernate.annotations.DynamicUpdate;

/**
 * 회원. 역할은 1개만 가진다.
 * 로그인 실패 횟수·잠금은 동시성 때문에 UserRepository 의 원자 UPDATE 로 바꾼다.
 * 변경된 컬럼만 UPDATE 해(@DynamicUpdate) 역할 변경 등이 동시에 바뀐 실패 횟수·잠금을 옛 값으로 덮어쓰지 않게 한다.
 */
@Entity
@DynamicUpdate
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
