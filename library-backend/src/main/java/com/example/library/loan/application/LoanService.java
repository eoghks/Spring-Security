package com.example.library.loan.application;

import com.example.library.book.domain.Book;
import com.example.library.book.domain.BookRepository;
import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import com.example.library.loan.api.LoanResponse;
import com.example.library.loan.domain.Loan;
import com.example.library.loan.domain.LoanPolicy;
import com.example.library.loan.domain.LoanRepository;
import com.example.library.user.domain.User;
import com.example.library.user.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대출·반납 처리. 회원 본인 대출과 사서 대행 대출이 같은 규칙을 쓴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {

	private final LoanRepository loanRepository;
	private final BookRepository bookRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	/**
	 * 대출한다. 같은 사용자의 동시 대출이 5권 제한을 넘지 않도록 사용자 행을 잠근 뒤 검사한다.
	 */
	public LoanResponse borrow(Long userId, Long bookId) {
		userRepository.findForUpdate(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		LocalDate today = LocalDate.now(clock);
		LoanPolicy.validateBorrow(
				loanRepository.countByUserIdAndReturnedDateIsNull(userId),
				loanRepository.existsByUserIdAndReturnedDateIsNullAndDueDateBefore(userId, today),
				loanRepository.existsByUserIdAndBookIdAndReturnedDateIsNull(userId, bookId));
		if (!bookRepository.existsById(bookId)) {
			throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
		}
		if (bookRepository.decrementAvailable(bookId) == 0) {
			throw new BusinessException(ErrorCode.BOOK_NOT_AVAILABLE);
		}
		// 조건부 UPDATE 가 영속성 컨텍스트를 비우므로 참조를 다시 얻는다
		User user = userRepository.getReferenceById(userId);
		Book book = bookRepository.getReferenceById(bookId);
		Loan loan = loanRepository.save(Loan.start(user, book, today));
		return LoanResponse.of(loan, today);
	}

	/**
	 * 본인 대출 반납. 남의 대출은 존재 여부를 드러내지 않도록 LOAN_NOT_FOUND 로 응답한다.
	 * 반납은 대출 행을 잠근 뒤 판정해, 같은 대출을 동시에 반납해도 재고는 한 번만 늘고 나머지는 409 ALREADY_RETURNED 다.
	 */
	public LoanResponse returnMine(Long userId, Long loanId) {
		Loan loan = loanRepository.findForUpdate(loanId)
				.filter(found -> found.getUser().getId().equals(userId))
				.orElseThrow(() -> new BusinessException(ErrorCode.LOAN_NOT_FOUND));
		return returnLoan(loan);
	}

	/** 사서 반납 처리(본인 반납과 같은 행 잠금으로 직렬화) */
	public LoanResponse returnAny(Long loanId) {
		Loan loan = loanRepository.findForUpdate(loanId)
				.orElseThrow(() -> new BusinessException(ErrorCode.LOAN_NOT_FOUND));
		return returnLoan(loan);
	}

	@Transactional(readOnly = true)
	public List<LoanResponse> myLoans(Long userId) {
		LocalDate today = LocalDate.now(clock);
		return loanRepository.findByUserIdOrderByIdDesc(userId).stream()
				.map(loan -> LoanResponse.of(loan, today))
				.toList();
	}

	private LoanResponse returnLoan(Loan loan) {
		LocalDate today = LocalDate.now(clock);
		loan.returnBook(today);
		// 조건부 UPDATE 가 영속성 컨텍스트를 비우므로 응답을 먼저 만든다
		LoanResponse response = LoanResponse.of(loan, today);
		Long bookId = loan.getBook().getId();
		if (bookRepository.incrementAvailable(bookId) == 0) {
			// 반납은 정상 처리하되(회원이 반납하지 못하면 안 되므로), 재고가 이미 보유 수량이면 늘리지 않고 불일치를 남긴다
			log.warn("반납 처리했지만 재고가 이미 보유 수량과 같아 늘리지 않음(재고 불일치 의심): loanId={}, bookId={}",
					loan.getId(), bookId);
		}
		return response;
	}
}
