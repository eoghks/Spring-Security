package com.example.library.authz.api;

import com.example.library.authz.domain.Role;

/**
 * 역할 응답.
 *
 * @param system 권한 편집이 막힌 시스템 역할(ADMIN) 여부
 */
public record RoleResponse(Long id, String code, String name, boolean system) {

	public static RoleResponse from(Role role) {
		return new RoleResponse(role.getId(), role.getCode(), role.getName(), role.isSystemAdmin());
	}
}
