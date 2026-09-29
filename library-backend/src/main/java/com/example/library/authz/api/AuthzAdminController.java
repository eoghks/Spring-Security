package com.example.library.authz.api;

import com.example.library.authz.api.MenuTreeResponse.UrlNode;
import com.example.library.authz.application.AuthzAdminService;
import com.example.library.authz.rule.AuthorizationRuleBroadcaster;
import com.example.library.security.LibraryPrincipal;
import com.example.library.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메뉴·액션·URL 매핑 관리 API.
 * 메뉴 트리 조회는 ROLE_MANAGE:READ 또는 API_KEY:READ(OR), URL 편집·재적재는 ROLE_MANAGE:URL_EDIT.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AuthzAdminController {

	private final AuthzAdminService authzAdminService;
	private final AuthorizationRuleBroadcaster ruleBroadcaster;

	@GetMapping("/menus")
	public List<MenuTreeResponse> menus() {
		return authzAdminService.menuTree();
	}

	/** 액션에 URL 추가 — 커밋 후 모든 노드가 규칙을 다시 적재한다 */
	@PostMapping("/actions/{id}/urls")
	@ResponseStatus(HttpStatus.CREATED)
	public UrlNode addActionUrl(@AuthenticationPrincipal LibraryPrincipal actor, @PathVariable Long id,
			@Valid @RequestBody ActionUrlRequest request) {
		return authzAdminService.addActionUrl(UserPrincipal.require(actor), id, request);
	}

	@DeleteMapping("/action-urls/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteActionUrl(@AuthenticationPrincipal LibraryPrincipal actor, @PathVariable Long id) {
		authzAdminService.deleteActionUrl(UserPrincipal.require(actor), id);
	}

	/** DB 를 직접 고친 경우 등 수동으로 규칙을 다시 적재한다(전 노드) */
	@PostMapping("/authz/reload")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void reload() {
		ruleBroadcaster.reloadAndBroadcast();
	}
}
