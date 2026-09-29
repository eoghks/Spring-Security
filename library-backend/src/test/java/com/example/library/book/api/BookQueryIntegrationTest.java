package com.example.library.book.api;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookQueryIntegrationTest extends IntegrationTestSupport {

	@Test
	@DisplayName("키워드·분류로 검색하고 페이지 정보를 돌려준다")
	void search() throws Exception {
		String member = bearer("member", MEMBER_PASSWORD);

		mockMvc.perform(get("/api/books").param("keyword", "클린").header("Authorization", member))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[0].title").value("클린 아키텍처"));
		mockMvc.perform(get("/api/books").param("category", "소설").param("size", "2").header("Authorization", member))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.totalElements").value(5))
				.andExpect(jsonPath("$.totalPages").value(3));
	}

	@Test
	@DisplayName("도서 상세와 분류 목록을 조회한다")
	void detailAndCategories() throws Exception {
		String member = bearer("member", MEMBER_PASSWORD);

		mockMvc.perform(get("/api/books/1").header("Authorization", member))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isbn").exists());
		mockMvc.perform(get("/api/books/categories").header("Authorization", member))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasItem("IT")));
		mockMvc.perform(get("/api/books/999999").header("Authorization", member))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"));
	}

	@Test
	@DisplayName("사서는 BOOK:READ 가 없어도 BOOK_MANAGE:READ 로 같은 URL 을 호출한다(OR)")
	void librarianCanReadByOtherAction() throws Exception {
		mockMvc.perform(get("/api/books").header("Authorization", bearer("librarian", LIBRARIAN_PASSWORD)))
				.andExpect(status().isOk());
	}
}
