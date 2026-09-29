package com.example.library.loan.domain;

import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import java.time.LocalDate;

/**
 * 대출 규칙. 1인 최대 5권, 연체 중이면 대출 불가, 같은 도서 중복 대출 불가, 대출 기간 14일.
 */
public final class LoanPolicy {

	public static final int MAX_ACTIVE_LOANS = 5;
	public static final int LOAN_DAYS = 14;

	private LoanPolicy() {
	}

	/**
	 * 대출 가능 여부를 검사한다. 위반 시 업무 예외를 던진다.
	 *
	 * @param activeLoanCount       미반납 대출 권수
	 * @param hasOverdue            연체 중인 대출 존재 여부
	 * @param alreadyBorrowingSame  같은 도서를 이미 대출 중인지
	 */
	public static void validateBorrow(long activeLoanCount, boolean hasOverdue, boolean alreadyBorrowingSame) {
		if (hasOverdue) {
			throw new BusinessException(ErrorCode.OVERDUE_LOAN_EXISTS);
		}
		if (activeLoanCount >= MAX_ACTIVE_LOANS) {
			throw new BusinessException(ErrorCode.LOAN_LIMIT_EXCEEDED);
		}
		if (alreadyBorrowingSame) {
			throw new BusinessException(ErrorCode.ALREADY_BORROWED);
		}
	}

	public static LocalDate dueDate(LocalDate loanDate) {
		return loanDate.plusDays(LOAN_DAYS);
	}
}
