package com.example.library.apikey.api;

import com.example.library.apikey.application.ApiKeyService;
import com.example.library.security.LibraryPrincipal;
import com.example.library.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Key 관리 API(API_KEY:READ / ISSUE / REVOKE).
 */
@RestController
@RequestMapping("/api/admin/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

	private final ApiKeyService apiKeyService;

	@GetMapping
	public List<ApiKeyResponse> list() {
		return apiKeyService.list();
	}

	/** 발급 — 응답의 apiKey 원문은 이번 한 번만 제공된다. 발급자는 사용자여야 한다(API Key 로 키 발급 불가) */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public IssuedApiKeyResponse issue(@AuthenticationPrincipal LibraryPrincipal issuer,
			@Valid @RequestBody ApiKeyIssueRequest request) {
		return apiKeyService.issue(UserPrincipal.require(issuer), request);
	}

	@PostMapping("/{id}/revoke")
	public ApiKeyResponse revoke(@PathVariable Long id) {
		return apiKeyService.revoke(id);
	}
}
