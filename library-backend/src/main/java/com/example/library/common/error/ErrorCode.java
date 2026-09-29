package com.example.library.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * API 오류 코드. 클라이언트는 message 가 아니라 code 로 분기한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// 인증(401)
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "액세스 토큰이 만료되었습니다."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
	ACCOUNT_LOCKED(HttpStatus.UNAUTHORIZED, "로그인 실패가 반복되어 계정이 잠겼습니다. 관리자에게 문의하세요."),
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
	INVALID_API_KEY(HttpStatus.UNAUTHORIZED, "유효하지 않은 API Key 입니다."),

	// 인가(403)
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	ACCESS_CONDITION_DENIED(HttpStatus.FORBIDDEN, "허용된 접속 조건이 아닙니다."),

	// 요청 오류(400)
	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
	INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "보유 수량은 대출 중인 수량보다 작을 수 없습니다."),
	INVALID_URL_PATTERN(HttpStatus.BAD_REQUEST, "URL 패턴 형식이 올바르지 않습니다."),
	SYSTEM_ROLE_PROTECTED(HttpStatus.BAD_REQUEST, "관리자 역할의 권한은 변경할 수 없습니다."),
	INVALID_ACTION(HttpStatus.BAD_REQUEST, "존재하지 않는 액션이 포함되어 있습니다."),

	// 대상 없음(404)
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
	ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "역할을 찾을 수 없습니다."),
	BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "도서를 찾을 수 없습니다."),
	LOAN_NOT_FOUND(HttpStatus.NOT_FOUND, "대출 정보를 찾을 수 없습니다."),
	ACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "액션을 찾을 수 없습니다."),
	ACTION_URL_NOT_FOUND(HttpStatus.NOT_FOUND, "액션 URL 을 찾을 수 없습니다."),
	API_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "API Key 를 찾을 수 없습니다."),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),

	// 메서드 불일치(405)
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),

	// 업무 규칙 충돌(409)
	DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
	DUPLICATE_ISBN(HttpStatus.CONFLICT, "이미 등록된 ISBN 입니다."),
	DUPLICATE_ACTION_URL(HttpStatus.CONFLICT, "이미 등록된 액션 URL 입니다."),
	LOAN_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "최대 대출 권수를 초과했습니다."),
	OVERDUE_LOAN_EXISTS(HttpStatus.CONFLICT, "연체 중인 도서가 있어 대출할 수 없습니다."),
	BOOK_NOT_AVAILABLE(HttpStatus.CONFLICT, "대출 가능한 재고가 없습니다."),
	ALREADY_BORROWED(HttpStatus.CONFLICT, "이미 대출 중인 도서입니다."),
	ALREADY_RETURNED(HttpStatus.CONFLICT, "이미 반납된 대출입니다."),
	BOOK_HAS_ACTIVE_LOANS(HttpStatus.CONFLICT, "대출 중인 도서는 삭제할 수 없습니다."),
	BOOK_HAS_LOAN_HISTORY(HttpStatus.CONFLICT, "대출 이력이 있는 도서는 삭제할 수 없습니다. 보유 수량을 0 으로 조정하세요."),
	API_KEY_ALREADY_REVOKED(HttpStatus.CONFLICT, "이미 폐기된 API Key 입니다."),

	// 서버 오류(500)
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String message;
}
