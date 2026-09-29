package com.example.library.book.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.support.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class BookCommandIntegrationTest extends IntegrationTestSupport {

	private static final String NEW_BOOK = """
			{"isbn":"9780000000001","title":"테스트 도서","author":"저자","publisher":"출판사","category":"IT","totalQuantity":2}
			""";

	@Test
	@DisplayName("사서는 도서를 등록·수정·삭제할 수 있다")
	void librarianManagesBooks() throws Exception {
		String librarian = bearer("librarian", LIBRARIAN_PASSWORD);

		String created = mockMvc.perform(post("/api/books").header("Authorization", librarian)
						.contentType(MediaType.APPLICATION_JSON).content(NEW_BOOK))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.availableQuantity").value(2))
				.andReturn().getResponse().getContentAsString();
		long id = objectMapper.readValue(created, JsonNode.class).get("id").asLong();

		mockMvc.perform(put("/api/books/" + id).header("Authorization", librarian)
						.contentType(MediaType.APPLICATION_JSON).content(NEW_BOOK.replace("\"totalQuantity\":2", "\"totalQuantity\":5")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalQuantity").value(5))
				.andExpect(jsonPath("$.availableQuantity").value(5));

		mockMvc.perform(delete("/api/books/" + id).header("Authorization", librarian))
				.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("일반 회원이 도서를 등록·삭제하면 403 ACCESS_DENIED")
	void memberCannotManageBooks() throws Exception {
		String member = bearer("member", MEMBER_PASSWORD);

		mockMvc.perform(post("/api/books").header("Authorization", member)
						.contentType(MediaType.APPLICATION_JSON).content(NEW_BOOK))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		mockMvc.perform(delete("/api/books/2").header("Authorization", member))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("대출 중인 도서는 삭제할 수 없고, 보유 수량을 대출 중 권수보다 줄일 수 없다")
	void cannotDeleteLoanedBook() throws Exception {
		String admin = bearer("admin", ADMIN_PASSWORD);

		// 시드: 클린 코드(id=1) 는 member 가 1권 대출 중
		mockMvc.perform(delete("/api/books/1").header("Authorization", admin))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BOOK_HAS_ACTIVE_LOANS"));
		mockMvc.perform(put("/api/books/1").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"isbn":"9788966260959","title":"클린 코드","author":"로버트 C. 마틴","publisher":"인사이트","category":"IT","totalQuantity":0}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_QUANTITY"));
	}
}
