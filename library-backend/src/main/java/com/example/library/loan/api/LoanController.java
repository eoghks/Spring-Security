package com.example.library.loan.api;

import com.example.library.loan.application.LoanService;
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
 * 회원 본인 대출 API(대출 신청 BOOK:BORROW, 내 대출 MY_LOAN:READ, 반납 MY_LOAN:RETURN).
 * "본인" 이 필요한 API 라 사용자 로그인으로만 호출할 수 있다(API Key 는 403 USER_ONLY).
 */
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

	private final LoanService loanService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public LoanResponse borrow(@AuthenticationPrincipal LibraryPrincipal principal,
			@Valid @RequestBody BorrowRequest request) {
		return loanService.borrow(UserPrincipal.require(principal).userId(), request.bookId());
	}

	@GetMapping("/me")
	public List<LoanResponse> myLoans(@AuthenticationPrincipal LibraryPrincipal principal) {
		return loanService.myLoans(UserPrincipal.require(principal).userId());
	}

	@PostMapping("/{id}/return")
	public LoanResponse returnBook(@AuthenticationPrincipal LibraryPrincipal principal, @PathVariable Long id) {
		return loanService.returnMine(UserPrincipal.require(principal).userId(), id);
	}
}
