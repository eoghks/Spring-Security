package com.example.library.loan.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 사서 대출 처리(회원 대신 대출).
 */
public record CheckoutRequest(
		@NotBlank(message = "회원 아이디를 입력하세요.") String username,
		@NotNull(message = "도서를 선택하세요.") Long bookId) {
}
