package com.example.library.user.api;

import com.example.library.user.domain.User;

/**
 * 내 정보.
 */
public record MeResponse(Long id, String username, String name, String email, String roleCode, String roleName) {

	public static MeResponse from(User user) {
		return new MeResponse(user.getId(), user.getUsername(), user.getName(), user.getEmail(),
				user.getRole().getCode(), user.getRole().getName());
	}
}
