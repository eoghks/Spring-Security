package com.example.library.loan.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoanPolicyTest {

	@Test
	@DisplayName("미반납 4권까지는 대출할 수 있다")
	void allowUnderLimit() {
		assertThatCode(() -> LoanPolicy.validateBorrow(4, false, false)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("미반납 5권이면 LOAN_LIMIT_EXCEEDED")
	void rejectAtLimit() {
		assertThatThrownBy(() -> LoanPolicy.validateBorrow(5, false, false))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.LOAN_LIMIT_EXCEEDED);
	}

	@Test
	@DisplayName("연체 중이면 권수와 무관하게 OVERDUE_LOAN_EXISTS")
	void rejectWhenOverdue() {
		assertThatThrownBy(() -> LoanPolicy.validateBorrow(0, true, false))
				.extracting("errorCode").isEqualTo(ErrorCode.OVERDUE_LOAN_EXISTS);
	}

	@Test
	@DisplayName("같은 도서를 이미 대출 중이면 ALREADY_BORROWED")
	void rejectDuplicateBook() {
		assertThatThrownBy(() -> LoanPolicy.validateBorrow(1, false, true))
				.extracting("errorCode").isEqualTo(ErrorCode.ALREADY_BORROWED);
	}

	@Test
	@DisplayName("반납 예정일은 대출일 + 14일이고, 그 다음 날부터 연체다")
	void dueDateAndOverdue() {
		LocalDate loanDate = LocalDate.of(2026, 3, 1);
		Loan loan = Loan.start(null, null, loanDate);

		assertThat(loan.getDueDate()).isEqualTo(LocalDate.of(2026, 3, 15));
		assertThat(loan.effectiveStatus(LocalDate.of(2026, 3, 15))).isEqualTo(LoanStatus.LOANED);
		assertThat(loan.effectiveStatus(LocalDate.of(2026, 3, 16))).isEqualTo(LoanStatus.OVERDUE);

		loan.returnBook(LocalDate.of(2026, 3, 20));
		assertThat(loan.effectiveStatus(LocalDate.of(2026, 3, 21))).isEqualTo(LoanStatus.RETURNED);
		assertThatThrownBy(() -> loan.returnBook(LocalDate.of(2026, 3, 21)))
				.extracting("errorCode").isEqualTo(ErrorCode.ALREADY_RETURNED);
	}
}
