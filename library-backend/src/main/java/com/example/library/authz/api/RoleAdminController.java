package com.example.library.authz.api;

import com.example.library.authz.domain.RoleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 역할 API. 역할 목록은 회원 관리(USER_MANAGE:READ)와 역할·권한 관리(ROLE_MANAGE:READ) 양쪽에서 쓴다(OR).
 */
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class RoleAdminController {

	private final RoleRepository roleRepository;

	@GetMapping
	@Transactional(readOnly = true)
	public List<RoleResponse> roles() {
		return roleRepository.findAllByOrderByIdAsc().stream().map(RoleResponse::from).toList();
	}
}
