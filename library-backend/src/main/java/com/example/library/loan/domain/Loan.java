package com.example.library.loan.domain;

import com.example.library.book.domain.Book;
import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import com.example.library.user.domain.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대출.
 */
@Entity
@Table(name = "loans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Loan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "book_id")
	private Book book;

	private LocalDate loanDate;

	private LocalDate dueDate;

	@Getter(AccessLevel.NONE)
	private LocalDate returnedDate;

	@Enumerated(EnumType.STRING)
	private LoanStatus status;

	private Loan(User user, Book book, LocalDate loanDate) {
		this.user = user;
		this.book = book;
		this.loanDate = loanDate;
		this.dueDate = LoanPolicy.dueDate(loanDate);
		this.status = LoanStatus.LOANED;
	}

	public static Loan start(User user, Book book, LocalDate today) {
		return new Loan(user, book, today);
	}

	public Optional<LocalDate> getReturnedDate() {
		return Optional.ofNullable(returnedDate);
	}

	public boolean isReturned() {
		return returnedDate != null;
	}

	/** 미반납이고 반납 예정일이 지났으면 연체 */
	public boolean isOverdue(LocalDate today) {
		return !isReturned() && today.isAfter(dueDate);
	}

	/** 조회 시점 기준 상태(배치가 아직 OVERDUE 로 바꾸지 않았어도 정확히 보이도록 계산) */
	public LoanStatus effectiveStatus(LocalDate today) {
		if (isReturned()) {
			return LoanStatus.RETURNED;
		}
		return isOverdue(today) ? LoanStatus.OVERDUE : LoanStatus.LOANED;
	}

	public void returnBook(LocalDate today) {
		if (isReturned()) {
			throw new BusinessException(ErrorCode.ALREADY_RETURNED);
		}
		this.returnedDate = today;
		this.status = LoanStatus.RETURNED;
	}
}
