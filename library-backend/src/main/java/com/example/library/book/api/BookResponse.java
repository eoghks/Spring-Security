package com.example.library.book.api;

import com.example.library.book.domain.Book;

/**
 * 도서 응답.
 */
public record BookResponse(Long id, String isbn, String title, String author, String publisher, String category,
		int totalQuantity, int availableQuantity) {

	public static BookResponse from(Book book) {
		return new BookResponse(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(), book.getPublisher(),
				book.getCategory(), book.getTotalQuantity(), book.getAvailableQuantity());
	}
}
