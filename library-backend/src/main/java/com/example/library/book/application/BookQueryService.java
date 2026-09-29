package com.example.library.book.application;

import com.example.library.book.api.BookResponse;
import com.example.library.book.domain.BookRepository;
import com.example.library.common.api.PageResponse;
import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 도서 조회.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookQueryService {

	private final BookRepository bookRepository;

	public PageResponse<BookResponse> search(String keyword, String category, int page, int size) {
		PageRequest pageable = PageRequest.of(Math.max(page, 0), PageResponse.clampSize(size), Sort.by("title"));
		return PageResponse.of(bookRepository.search(keyword.trim(), category.trim(), pageable), BookResponse::from);
	}

	public BookResponse get(Long id) {
		return bookRepository.findById(id)
				.map(BookResponse::from)
				.orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));
	}

	public List<String> categories() {
		return bookRepository.findCategories();
	}
}
