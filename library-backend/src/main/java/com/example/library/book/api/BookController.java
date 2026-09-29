package com.example.library.book.api;

import com.example.library.book.application.BookCommandService;
import com.example.library.book.application.BookQueryService;
import com.example.library.common.api.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 도서 API. 조회는 회원·사서·API Key 모두 쓸 수 있다(BOOK:READ 또는 BOOK_MANAGE:READ).
 * 등록·수정·삭제는 도서 관리 메뉴의 액션(BOOK_MANAGE:CREATE/UPDATE/DELETE)이 필요하다.
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

	private final BookQueryService bookQueryService;
	private final BookCommandService bookCommandService;

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

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BookResponse create(@Valid @RequestBody BookRequest request) {
		return bookCommandService.create(request);
	}

	@PutMapping("/{id}")
	public BookResponse update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
		return bookCommandService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		bookCommandService.delete(id);
	}
}
