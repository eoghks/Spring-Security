package com.example.library.common.error;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 컨트롤러 계층 예외를 공통 오류 응답으로 변환한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest request) {
		ErrorCode errorCode = e.getErrorCode();
		return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, request.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e,
			HttpServletRequest request) {
		List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
				.toList();
		return ResponseEntity.badRequest()
				.body(ErrorResponse.withFieldErrors(ErrorCode.VALIDATION_FAILED, request.getRequestURI(), fieldErrors));
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
			HandlerMethodValidationException.class})
	public ResponseEntity<ErrorResponse> handleBadRequest(Exception e, HttpServletRequest request) {
		log.debug("잘못된 요청: {}", e.getMessage());
		return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, request.getRequestURI()));
	}

	/**
	 * 스프링 MVC 표준 예외(404·405·필수 파라미터 누락 등)는 상태 코드에 맞춰 변환한다.
	 */
	@ExceptionHandler({ServletException.class, ErrorResponseException.class})
	public ResponseEntity<ErrorResponse> handleSpringMvc(Exception e, HttpServletRequest request) {
		ErrorCode errorCode = toErrorCode(e);
		return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, request.getRequestURI()));
	}

	private ErrorCode toErrorCode(Exception e) {
		int status = e instanceof org.springframework.web.ErrorResponse response
				? response.getStatusCode().value()
				: HttpStatus.INTERNAL_SERVER_ERROR.value();
		return switch (status) {
			case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
			case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
			case 500 -> ErrorCode.INTERNAL_ERROR;
			default -> ErrorCode.VALIDATION_FAILED;
		};
	}

	/**
	 * 동시 요청 경합으로 유니크 제약에 걸린 경우를 409 로 번역한다. 알 수 없는 제약 위반은 그대로 500 이다.
	 * 드라이버 메시지에는 입력값(아이디·이메일 등)이 들어 있으므로 500 로그에도 메시지·스택 대신 제약 이름만 남긴다.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e,
			HttpServletRequest request) {
		return DataIntegrityErrorTranslator.translate(e)
				.map(errorCode -> {
					log.info("유니크 제약 위반을 {} 로 응답: {}", errorCode, request.getRequestURI());
					return ResponseEntity.status(errorCode.getStatus())
							.body(ErrorResponse.of(errorCode, request.getRequestURI()));
				})
				.orElseGet(() -> {
					log.error("처리되지 않은 무결성 제약 위반: {} (제약={})", request.getRequestURI(),
							DataIntegrityErrorTranslator.constraintName(e).orElse("알 수 없음"));
					return ResponseEntity.internalServerError()
							.body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, request.getRequestURI()));
				});
	}

	/** 낙관적 락 충돌(예: 도서 수정 중 대출·반납이 먼저 커밋됨)은 덮어쓰지 않고 409 로 알린다 */
	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException e,
			HttpServletRequest request) {
		log.info("동시 수정 충돌을 409 로 응답: {}", request.getRequestURI());
		return ResponseEntity.status(ErrorCode.CONCURRENT_MODIFICATION.getStatus())
				.body(ErrorResponse.of(ErrorCode.CONCURRENT_MODIFICATION, request.getRequestURI()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
		log.error("처리되지 않은 예외: {}", request.getRequestURI(), e);
		return ResponseEntity.internalServerError()
				.body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, request.getRequestURI()));
	}
}
