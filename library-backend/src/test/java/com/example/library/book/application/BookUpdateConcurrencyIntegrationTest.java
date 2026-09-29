package com.example.library.book.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.library.book.api.BookRequest;
import com.example.library.book.api.BookResponse;
import com.example.library.book.domain.Book;
import com.example.library.book.domain.BookRepository;
import com.example.library.loan.application.LoanService;
import com.example.library.support.IntegrationTestSupport;
import com.example.library.user.domain.UserRepository;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 도서 수정과 대출(재고 감소)이 겹칠 때 수정이 재고 변경을 덮어쓰지 않는지(lost update 방지) 검증한다.
 */
class BookUpdateConcurrencyIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private BookCommandService bookCommandService;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private LoanService loanService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Test
	@DisplayName("수정 요청이 도서를 읽은 뒤 대출이 먼저 커밋되면 수정은 낙관적 락 충돌로 거절되고 대출 재고가 유지된다")
	void updateAfterConcurrentBorrowIsRejected() throws Exception {
		BookResponse book = bookCommandService.create(request("9780000000901", "경합 도서", 2));
		signup("bookrace01", "Passw0rd!");
		Long userId = userRepository.findByUsername("bookrace01").orElseThrow().getId();

		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(tx -> {
			// 수정 요청이 도서를 읽어 둔 시점(가용 2)
			bookRepository.findById(book.id()).orElseThrow();
			// 그 사이 다른 요청(스레드)의 대출이 커밋된다(가용 1)
			CompletableFuture.runAsync(() -> loanService.borrow(userId, book.id())).join();
			bookCommandService.update(book.id(), request("9780000000901", "경합 도서(제목 수정)", 2));
		})).isInstanceOf(OptimisticLockingFailureException.class);

		Book reloaded = bookRepository.findById(book.id()).orElseThrow();
		assertThat(reloaded.getAvailableQuantity()).isEqualTo(1);
		assertThat(reloaded.getTitle()).isEqualTo("경합 도서");
	}

	@Test
	@DisplayName("겹치는 변경이 없으면 수정은 그대로 성공한다")
	void updateWithoutConflictSucceeds() {
		BookResponse book = bookCommandService.create(request("9780000000902", "단독 수정", 2));

		BookResponse updated = bookCommandService.update(book.id(), request("9780000000902", "단독 수정(완료)", 3));

		assertThat(updated.title()).isEqualTo("단독 수정(완료)");
		assertThat(updated.availableQuantity()).isEqualTo(3);
	}

	private BookRequest request(String isbn, String title, int totalQuantity) {
		return new BookRequest(isbn, title, "저자", "출판사", "IT", totalQuantity);
	}
}
