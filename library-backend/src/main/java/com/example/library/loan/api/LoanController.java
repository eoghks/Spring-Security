package com.example.library.loan.api;

import com.example.library.loan.application.LoanService;
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
 */
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

	private final LoanService loanService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public LoanResponse borrow(@AuthenticationPrincipal UserPrincipal principal,
			@Valid @RequestBody BorrowRequest request) {
		return loanService.borrow(principal.userId(), request.bookId());
	}

	@GetMapping("/me")
	public List<LoanResponse> myLoans(@AuthenticationPrincipal UserPrincipal principal) {
		return loanService.myLoans(principal.userId());
	}

	@PostMapping("/{id}/return")
	public LoanResponse returnBook(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
		return loanService.returnMine(principal.userId(), id);
	}
}
