package com.example.library.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

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
}
