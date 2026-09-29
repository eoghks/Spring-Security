package com.example.library.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	@DisplayName("낙관적 락 충돌은 409 CONCURRENT_MODIFICATION 으로 응답한다")
	void optimisticLockConflictIs409() {
		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/books/1");

		ResponseEntity<ErrorResponse> response = handler.handleOptimisticLock(
				new ObjectOptimisticLockingFailureException("Book", 1L), request);

		assertThat(response.getStatusCode().value()).isEqualTo(409);
		assertThat(response.getBody().code()).isEqualTo("CONCURRENT_MODIFICATION");
	}

	@Test
	@DisplayName("알 수 없는 무결성 제약 위반은 500 이고, 로그에 입력값이 담긴 드라이버 메시지를 남기지 않는다")
	void unknownConstraintViolationLogsWithoutValues(CapturedOutput output) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/signup");
		SQLException sql = new SQLException("ERROR: violates check constraint \"ck_x\" VALUES ('secret-value@example.com')");

		ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(
				new DataIntegrityViolationException("could not execute statement 'secret-value@example.com'", sql), request);

		assertThat(response.getStatusCode().value()).isEqualTo(500);
		assertThat(output).contains("처리되지 않은 무결성 제약 위반").doesNotContain("secret-value");
	}
}
