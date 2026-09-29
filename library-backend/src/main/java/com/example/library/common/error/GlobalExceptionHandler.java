package com.example.library.common.error;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
		log.error("처리되지 않은 예외: {}", request.getRequestURI(), e);
		return ResponseEntity.internalServerError()
				.body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, request.getRequestURI()));
	}
}
