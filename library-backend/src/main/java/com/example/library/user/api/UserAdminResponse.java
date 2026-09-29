package com.example.library.user.api;

import com.example.library.user.domain.User;
import java.time.LocalDateTime;

/**
 * 회원 관리 목록 응답.
 */
public record UserAdminResponse(Long id, String username, String name, String email, Long roleId, String roleCode,
		String roleName, boolean locked, int failedLoginCount, LocalDateTime createdAt) {

	public static UserAdminResponse from(User user) {
		return new UserAdminResponse(user.getId(), user.getUsername(), user.getName(), user.getEmail(),
				user.getRole().getId(), user.getRole().getCode(), user.getRole().getName(), user.isLocked(),
				user.getFailedLoginCount(), user.getCreatedAt());
	}
}
