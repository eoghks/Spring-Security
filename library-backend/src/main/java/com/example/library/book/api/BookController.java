package com.example.library.book.api;

import com.example.library.book.application.BookQueryService;
import com.example.library.common.api.PageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 도서 API. 조회는 회원·사서·API Key 모두 쓸 수 있다(BOOK:READ 또는 BOOK_MANAGE:READ).
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

	private final BookQueryService bookQueryService;

	@GetMapping
	public PageResponse<BookResponse> search(
			@RequestParam(defaultValue = "") String keyword,
			@RequestParam(defaultValue = "") String category,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return bookQueryService.search(keyword, category, page, size);
	}

	@GetMapping("/{id}")
	public BookResponse get(@PathVariable Long id) {
		return bookQueryService.get(id);
	}

	@GetMapping("/categories")
	public List<String> categories() {
		return bookQueryService.categories();
	}
}
