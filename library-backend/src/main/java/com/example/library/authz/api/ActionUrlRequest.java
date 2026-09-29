package com.example.library.authz.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 액션 URL 추가 요청.
 * URL 패턴은 PathPattern 문법 중 안전한 부분만 허용한다: 리터럴 세그먼트, {변수}, *, 그리고 끝의 /** 만.
 */
public record ActionUrlRequest(
		@NotBlank(message = "HTTP 메서드를 선택하세요.")
		@Pattern(regexp = "^(GET|POST|PUT|PATCH|DELETE)$", message = "HTTP 메서드는 GET/POST/PUT/PATCH/DELETE 입니다.")
		String httpMethod,

		@NotBlank(message = "URL 패턴을 입력하세요.")
		@Size(max = 200, message = "URL 패턴은 200자 이하입니다.")
		@Pattern(regexp = "^(/([A-Za-z0-9._~-]+|\\{[A-Za-z][A-Za-z0-9]*\\}|\\*))+(/\\*\\*)?$",
				message = "URL 패턴 형식이 올바르지 않습니다(예: /api/books/{id}).")
		String urlPattern) {
}
