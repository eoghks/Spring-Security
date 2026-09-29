package com.example.library.authz.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 역할. 부여된 액션 ID 목록(role_actions)을 함께 가진다.
 */
@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role {

	/** 모든 액션을 가지는 시스템 역할 코드 — 권한 편집 대상에서 제외한다 */
	public static final String ADMIN_CODE = "ADMIN";

	/** 회원가입 시 자동 부여되는 역할 코드 */
	public static final String MEMBER_CODE = "MEMBER";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String code;

	private String name;

	@ElementCollection
	@CollectionTable(name = "role_actions", joinColumns = @JoinColumn(name = "role_id"))
	@Column(name = "action_id")
	private Set<Long> actionIds = new HashSet<>();

	public boolean isSystemAdmin() {
		return ADMIN_CODE.equals(code);
	}

	/** 부여 액션 전체를 교체한다 */
	public void replaceActions(Set<Long> newActionIds) {
		actionIds.clear();
		actionIds.addAll(newActionIds);
	}
}
