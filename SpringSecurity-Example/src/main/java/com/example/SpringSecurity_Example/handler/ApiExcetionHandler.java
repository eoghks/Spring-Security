package com.example.SpringSecurity_Example.handler;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.example.SpringSecurity_Example.model.ApiError;

@ControllerAdvice
public class ApiExcetionHandler extends ResponseEntityExceptionHandler {
	private ApiError createApiError(Exception e, HttpStatusCode httpStatusCode) {
		ApiError apiError = new ApiError();
		if(e instanceof MethodArgumentNotValidException ) {
			((MethodArgumentNotValidException) e).getBindingResult()
			.getAllErrors().stream().forEach(error -> apiError.getMsg().add(error.getDefaultMessage()));
		}
		apiError.setStatus(httpStatusCode.value());
		apiError.setHttpStatus(httpStatusCode);
		return apiError;
	}
	
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ApiError apiError = createApiError(ex, status);
		return super.handleExceptionInternal(ex, apiError, headers, status, request);
	}
}
