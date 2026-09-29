package com.example.library.loan.api;

import com.example.library.loan.domain.Loan;
import com.example.library.loan.domain.LoanStatus;
import java.time.LocalDate;

/**
 * 대출 응답. status 는 조회 시점 기준으로 계산한 상태다.
 *
 * @param returnedDate 반납일(미반납이면 null — JSON 표현용)
 */
public record LoanResponse(Long id, Long bookId, String bookTitle, Long userId, String username,
		LocalDate loanDate, LocalDate dueDate, LocalDate returnedDate, LoanStatus status) {

	public static LoanResponse of(Loan loan, LocalDate today) {
		return new LoanResponse(loan.getId(), loan.getBook().getId(), loan.getBook().getTitle(),
				loan.getUser().getId(), loan.getUser().getUsername(), loan.getLoanDate(), loan.getDueDate(),
				loan.getReturnedDate().orElse(null), loan.effectiveStatus(today));
	}
}
