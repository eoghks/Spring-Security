package com.example.library.loan.api;

import com.example.library.common.api.PageResponse;
import com.example.library.loan.application.LoanManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사서 대출 관리 API(LOAN_MANAGE:READ / CHECKOUT / RETURN).
 * 회원 본인 API(/api/loans)와 URL 을 분리해, URL 인가만으로 "본인 것만" 과 "전체" 를 구분한다.
 */
@RestController
@RequestMapping("/api/loan-management")
@RequiredArgsConstructor
public class LoanManagementController {

	private final LoanManagementService loanManagementService;

	@GetMapping
	public PageResponse<LoanResponse> search(
			@RequestParam(defaultValue = "") String status,
			@RequestParam(defaultValue = "") String keyword,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return loanManagementService.search(status, keyword, page, size);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public LoanResponse checkout(@Valid @RequestBody CheckoutRequest request) {
		return loanManagementService.checkout(request);
	}

	@PostMapping("/{id}/return")
	public LoanResponse processReturn(@PathVariable Long id) {
		return loanManagementService.processReturn(id);
	}
}
