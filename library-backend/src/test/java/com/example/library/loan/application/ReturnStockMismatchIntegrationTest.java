package com.example.library.loan.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.book.api.BookRequest;
import com.example.library.book.application.BookCommandService;
import com.example.library.book.domain.BookRepository;
import com.example.library.support.IntegrationTestSupport;
import com.example.library.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 반납 시 재고 증가가 적용되지 않는(이미 보유 수량과 같은) 불일치를 조용히 넘기지 않는지 검증한다.
 */
@ExtendWith(OutputCaptureExtension.class)
class ReturnStockMismatchIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private BookCommandService bookCommandService;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private LoanService loanService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("재고가 이미 보유 수량이면 반납은 성공하되 재고는 그대로 두고 불일치 경고를 남긴다")
	void returnWithFullStockLogsWarning(CapturedOutput output) throws Exception {
		long bookId = bookCommandService.create(new BookRequest("9780000000301", "불일치 도서", "저자", "출판사", "IT", 1)).id();
		signup("mismatch01", "Passw0rd!");
		Long userId = userRepository.findByUsername("mismatch01").orElseThrow().getId();
		long loanId = loanService.borrow(userId, bookId).id();
		// 외부 요인(수동 DB 수정 등)으로 재고가 이미 채워진 상황
		jdbcTemplate.update("UPDATE books SET available_quantity = total_quantity WHERE id = ?", bookId);

		loanService.returnMine(userId, loanId);

		assertThat(bookRepository.findById(bookId).orElseThrow().getAvailableQuantity()).isEqualTo(1);
		assertThat(output).contains("재고 불일치 의심").contains("loanId=" + loanId);
	}
}
