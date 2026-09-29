package com.example.library.book.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 도서 등록·수정 요청.
 */
public record BookRequest(
		@NotBlank(message = "ISBN 을 입력하세요.")
		@Pattern(regexp = "^\\d{10}(\\d{3})?$", message = "ISBN 은 숫자 10자리 또는 13자리입니다.")
		String isbn,

		@NotBlank(message = "제목을 입력하세요.") @Size(max = 200, message = "제목은 200자 이하입니다.")
		String title,

		@NotBlank(message = "저자를 입력하세요.") @Size(max = 100, message = "저자는 100자 이하입니다.")
		String author,

		@NotBlank(message = "출판사를 입력하세요.") @Size(max = 100, message = "출판사는 100자 이하입니다.")
		String publisher,

		@NotBlank(message = "분류를 입력하세요.") @Size(max = 50, message = "분류는 50자 이하입니다.")
		String category,

		@Min(value = 0, message = "보유 수량은 0 이상입니다.") @Max(value = 1000, message = "보유 수량은 1000 이하입니다.")
		int totalQuantity) {
}
