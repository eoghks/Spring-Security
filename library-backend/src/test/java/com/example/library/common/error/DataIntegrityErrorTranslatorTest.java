package com.example.library.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class DataIntegrityErrorTranslatorTest {

	@Test
	@DisplayName("PostgreSQL 형식의 아이디 유니크 위반은 DUPLICATE_USERNAME")
	void postgresMessage() {
		SQLException sql = new SQLException(
				"ERROR: duplicate key value violates unique constraint \"uk_users_username\"", "23505");

		assertThat(DataIntegrityErrorTranslator.translate(new DataIntegrityViolationException("중복", sql)))
				.contains(ErrorCode.DUPLICATE_USERNAME);
	}

	@Test
	@DisplayName("Hibernate 가 추출한 제약 이름으로도 식별한다(H2 는 인덱스 이름이 붙어 대소문자가 섞인다)")
	void hibernateConstraintName() {
		ConstraintViolationException cause = new ConstraintViolationException("could not execute statement",
				new SQLException("Unique index or primary key violation"), "PUBLIC.UK_USERS_USERNAME_INDEX_6");

		assertThat(DataIntegrityErrorTranslator.translate(new DataIntegrityViolationException("중복", cause)))
				.contains(ErrorCode.DUPLICATE_USERNAME);
	}

	@Test
	@DisplayName("알 수 없는 제약 위반은 번역하지 않는다(500 유지)")
	void unknownConstraint() {
		SQLException sql = new SQLException("ERROR: insert or update violates foreign key constraint \"fk_x\"");

		assertThat(DataIntegrityErrorTranslator.translate(new DataIntegrityViolationException("무결성", sql)))
				.isEmpty();
	}
}
