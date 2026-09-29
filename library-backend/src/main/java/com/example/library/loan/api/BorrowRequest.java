package com.example.library.loan.api;

import jakarta.validation.constraints.NotNull;

/**
 * 대출 신청(본인).
 */
public record BorrowRequest(@NotNull(message = "도서를 선택하세요.") Long bookId) {
}
