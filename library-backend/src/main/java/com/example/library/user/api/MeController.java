package com.example.library.user.api;

import com.example.library.authz.application.PermissionQueryService;
import com.example.library.authz.application.PermissionQueryService.MyPermissions;
import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import com.example.library.security.UserPrincipal;
import com.example.library.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인 사용자 본인 정보. authenticated_urls 로 로그인만 요구한다.
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

	private final UserRepository userRepository;
	private final PermissionQueryService permissionQueryService;

	@GetMapping
	@Transactional(readOnly = true)
	public MeResponse me(@AuthenticationPrincipal UserPrincipal principal) {
		return userRepository.findWithRoleById(principal.userId())
				.map(MeResponse::from)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	/** 호출 가능한 URL 과 진입 가능한 메뉴 */
	@GetMapping("/permissions")
	public MyPermissions permissions(@AuthenticationPrincipal UserPrincipal principal) {
		return permissionQueryService.permissionsOf(principal);
	}
}
