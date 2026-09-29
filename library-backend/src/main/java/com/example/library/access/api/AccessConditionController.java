package com.example.library.access.api;

import com.example.library.access.application.AccessConditionService;
import com.example.library.common.api.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 접속 조건 관리 API(ACCESS_CONDITION:READ / SAVE / DELETE).
 */
@RestController
@RequestMapping("/api/admin/access-conditions")
@RequiredArgsConstructor
public class AccessConditionController {

	private final AccessConditionService accessConditionService;

	@GetMapping
	public PageResponse<AccessConditionResponse> list(
			@RequestParam(defaultValue = "") String keyword,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return accessConditionService.list(keyword, page, size);
	}

	@GetMapping("/{userId}")
	public AccessConditionResponse get(@PathVariable Long userId) {
		return accessConditionService.get(userId);
	}

	@PutMapping("/{userId}")
	public AccessConditionResponse save(@PathVariable Long userId, @Valid @RequestBody AccessConditionRequest request) {
		return accessConditionService.save(userId, request);
	}

	@DeleteMapping("/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long userId) {
		accessConditionService.delete(userId);
	}
}
