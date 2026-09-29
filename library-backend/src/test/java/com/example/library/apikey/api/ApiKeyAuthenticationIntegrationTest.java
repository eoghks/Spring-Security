package com.example.library.apikey.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ApiKeyAuthenticationIntegrationTest extends IntegrationTestSupport {

	/** data.sql 시드 샘플 키(BOOK:READ) — 개발용 */
	static final String SAMPLE_KEY = "lib_Elsu1z3_KkwNJYk7v7R7BxLaFBJpJV4qc61GHDjZvNY";

	@Test
	@DisplayName("API Key 로 도서 목록·상세를 조회한다")
	void readBooksWithApiKey() throws Exception {
		mockMvc.perform(get("/api/books").header("X-API-KEY", SAMPLE_KEY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
		mockMvc.perform(get("/api/books/2").header("X-API-KEY", SAMPLE_KEY))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("부여되지 않은 액션·로그인 전용 URL 은 403")
	void forbiddenWithoutAction() throws Exception {
		mockMvc.perform(post("/api/books").header("X-API-KEY", SAMPLE_KEY).contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		mockMvc.perform(get("/api/me").header("X-API-KEY", SAMPLE_KEY))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("무효 키는 즉시 401 INVALID_API_KEY — 유효한 JWT 가 함께 있어도 폴백하지 않는다")
	void invalidKey() throws Exception {
		mockMvc.perform(get("/api/books").header("X-API-KEY", "lib_short"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_API_KEY"));
		mockMvc.perform(get("/api/books").header("X-API-KEY", "lib_" + "A".repeat(43)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_API_KEY"));
		mockMvc.perform(get("/api/books").header("X-API-KEY", "wrong")
						.header("Authorization", bearer("member", MEMBER_PASSWORD)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_API_KEY"));
	}
}
