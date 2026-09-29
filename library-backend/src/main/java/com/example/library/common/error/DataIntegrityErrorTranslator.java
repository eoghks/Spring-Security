package com.example.library.common.error;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * DB 무결성 제약 위반을 업무 오류 코드로 번역한다.
 * 사전 중복 검사를 통과한 동시 요청이 유니크 제약에 걸리는 경합을 500 대신 409 로 돌려주기 위함이다.
 * 제약 이름은 schema.sql 에서 명시적으로 붙인 이름으로 식별한다(H2·PostgreSQL 공통).
 */
public final class DataIntegrityErrorTranslator {

	/** 제약 이름(소문자) → 오류 코드 */
	private static final Map<String, ErrorCode> CONSTRAINT_ERROR_CODES = Map.of(
			"uk_users_username", ErrorCode.DUPLICATE_USERNAME);

	private DataIntegrityErrorTranslator() {
	}

	/** 알려진 제약 위반이면 대응 오류 코드, 아니면 빈 값 */
	public static Optional<ErrorCode> translate(DataIntegrityViolationException e) {
		String detail = violationDetail(e).toLowerCase(Locale.ROOT);
		return CONSTRAINT_ERROR_CODES.entrySet().stream()
				.filter(entry -> detail.contains(entry.getKey()))
				.map(Map.Entry::getValue)
				.findFirst();
	}

	/** Hibernate 가 추출한 제약 이름과 드라이버 원본 메시지를 합친다(DB 마다 이름 추출 형식이 달라서) */
	private static String violationDetail(DataIntegrityViolationException e) {
		String constraintName = Optional.ofNullable(e.getCause())
				.filter(ConstraintViolationException.class::isInstance)
				.map(cause -> ((ConstraintViolationException) cause).getConstraintName())
				.orElse("");
		String message = Optional.ofNullable(e.getMostSpecificCause().getMessage()).orElse("");
		return constraintName + " " + message;
	}
}
