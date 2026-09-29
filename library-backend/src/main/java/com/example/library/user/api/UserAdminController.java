package com.example.library.user.api;

import com.example.library.common.api.PageResponse;
import com.example.library.security.LibraryPrincipal;
import com.example.library.security.UserPrincipal;
import com.example.library.user.application.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 관리 API(USER_MANAGE:READ / CHANGE_ROLE / UNLOCK).
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

	private final UserAdminService userAdminService;

	@GetMapping
	public PageResponse<UserAdminResponse> search(
			@RequestParam(defaultValue = "") String keyword,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return userAdminService.search(keyword, page, size);
	}

	@PutMapping("/{id}/role")
	public UserAdminResponse changeRole(@AuthenticationPrincipal LibraryPrincipal actor, @PathVariable Long id,
			@Valid @RequestBody ChangeRoleRequest request) {
		return userAdminService.changeRole(UserPrincipal.require(actor).userId(), id, request.roleId());
	}

	@PostMapping("/{id}/unlock")
	public UserAdminResponse unlock(@PathVariable Long id) {
		return userAdminService.unlock(id);
	}
}
