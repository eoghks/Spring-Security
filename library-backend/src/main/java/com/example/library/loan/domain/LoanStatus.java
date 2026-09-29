package com.example.library.loan.domain;

/**
 * 대출 상태.
 */
public enum LoanStatus {
	/** 대출 중 */
	LOANED,
	/** 반납 완료 */
	RETURNED,
	/** 반납 예정일 경과(미반납) */
	OVERDUE
}
