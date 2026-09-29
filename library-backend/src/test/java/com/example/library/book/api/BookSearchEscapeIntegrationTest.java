package com.example.library.book.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.book.application.BookCommandService;
import com.example.library.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 검색어의 %·_ 가 LIKE 와일드카드가 아니라 글자 그대로 검색되는지 검증한다.
 */
class BookSearchEscapeIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private BookCommandService bookCommandService;

	@Test
	@DisplayName("검색어 %·_ 는 글자 그대로 일치하는 도서만 찾는다(전체가 나오지 않는다)")
	void wildcardCharactersAreLiteral() throws Exception {
		bookCommandService.create(new BookRequest("9780000000501", "이스케이프 100%_달성", "저자", "출판사", "ESCAPE", 1));
		bookCommandService.create(new BookRequest("9780000000502", "이스케이프 100점 달성", "저자", "출판사", "ESCAPE", 1));
		String member = bearer("member", MEMBER_PASSWORD);

		mockMvc.perform(get("/api/books").param("keyword", "100%").param("category", "ESCAPE")
						.header("Authorization", member))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].isbn").value("9780000000501"));
		mockMvc.perform(get("/api/books").param("keyword", "%_").param("category", "ESCAPE")
						.header("Authorization", member))
				.andExpect(jsonPath("$.content.length()").value(1));
		mockMvc.perform(get("/api/books").param("keyword", "_").param("category", "ESCAPE")
						.header("Authorization", member))
				.andExpect(jsonPath("$.content.length()").value(1));
	}
}
