package com.example.library.authz.api;

import com.example.library.authz.application.AuthzAdminService;
import com.example.library.authz.domain.RoleRepository;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 역할 API. 역할 목록은 회원 관리(USER_MANAGE:READ)와 역할·권한 관리(ROLE_MANAGE:READ) 양쪽에서 쓴다(OR).
 * 역할 액션 조회는 ROLE_MANAGE:READ, 저장은 ROLE_MANAGE:GRANT.
 */
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class RoleAdminController {

	private final RoleRepository roleRepository;
	private final AuthzAdminService authzAdminService;

	@GetMapping
	@Transactional(readOnly = true)
	public List<RoleResponse> roles() {
		return roleRepository.findAllByOrderByIdAsc().stream().map(RoleResponse::from).toList();
	}

	@GetMapping("/{id}/actions")
	public RoleActionsResponse roleActions(@PathVariable Long id) {
		return authzAdminService.roleActions(id);
	}

	/** 역할 부여 액션 전체 교체 — 커밋 후 해당 역할 캐시만 evict 되어 즉시 반영된다 */
	@PutMapping("/{id}/actions")
	public RoleActionsResponse replaceRoleActions(@PathVariable Long id, @Valid @RequestBody RoleActionsRequest request) {
		return authzAdminService.replaceRoleActions(id, request.actionIds());
	}
}
