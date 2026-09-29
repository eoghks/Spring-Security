package com.example.library.common.error;

import java.time.Instant;
import java.util.List;

/**
 * 공통 오류 응답 본문.
 *
 * @param code        오류 코드(ErrorCode 이름)
 * @param message     사용자 표시용 메시지
 * @param path        요청 경로
 * @param timestamp   발생 시각
 * @param fieldErrors 입력값 검증 오류 목록(없으면 빈 목록)
 */
public record ErrorResponse(String code, String message, String path, Instant timestamp,
		List<FieldError> fieldErrors) {

	/** 필드 단위 검증 오류 */
	public record FieldError(String field, String message) {
	}

	public static ErrorResponse of(ErrorCode errorCode, String path) {
		return new ErrorResponse(errorCode.name(), errorCode.getMessage(), path, Instant.now(), List.of());
	}

	public static ErrorResponse withFieldErrors(ErrorCode errorCode, String path, List<FieldError> fieldErrors) {
		return new ErrorResponse(errorCode.name(), errorCode.getMessage(), path, Instant.now(),
				List.copyOf(fieldErrors));
	}
}
