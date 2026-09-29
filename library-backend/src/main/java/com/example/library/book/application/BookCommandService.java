package com.example.library.book.application;

import com.example.library.book.api.BookRequest;
import com.example.library.book.api.BookResponse;
import com.example.library.book.domain.Book;
import com.example.library.book.domain.BookRepository;
import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import com.example.library.loan.domain.LoanRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 도서 등록·수정·삭제(도서 관리 메뉴).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BookCommandService {

	private final BookRepository bookRepository;
	private final LoanRepository loanRepository;
	private final Clock clock;

	public BookResponse create(BookRequest request) {
		if (bookRepository.existsByIsbn(request.isbn())) {
			throw new BusinessException(ErrorCode.DUPLICATE_ISBN);
		}
		Book book = Book.builder()
				.isbn(request.isbn())
				.title(request.title())
				.author(request.author())
				.publisher(request.publisher())
				.category(request.category())
				.totalQuantity(request.totalQuantity())
				.createdAt(LocalDateTime.now(clock))
				.build();
		return BookResponse.from(bookRepository.save(book));
	}

	public BookResponse update(Long id, BookRequest request) {
		Book book = find(id);
		if (bookRepository.existsByIsbnAndIdNot(request.isbn(), id)) {
			throw new BusinessException(ErrorCode.DUPLICATE_ISBN);
		}
		book.updateInfo(request.isbn(), request.title(), request.author(), request.publisher(), request.category());
		book.changeTotalQuantity(request.totalQuantity());
		return BookResponse.from(book);
	}

	public void delete(Long id) {
		Book book = find(id);
		if (book.loanedQuantity() > 0) {
			throw new BusinessException(ErrorCode.BOOK_HAS_ACTIVE_LOANS);
		}
		if (loanRepository.existsByBookId(id)) {
			throw new BusinessException(ErrorCode.BOOK_HAS_LOAN_HISTORY);
		}
		bookRepository.delete(book);
	}

	private Book find(Long id) {
		return bookRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));
	}
}
