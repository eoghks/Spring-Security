package com.example.library.loan.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.support.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class LoanManagementIntegrationTest extends IntegrationTestSupport {

	@Test
	@DisplayName("사서는 회원 대신 대출을 처리하고 반납 처리할 수 있다")
	void checkoutAndReturn() throws Exception {
		signup("mgmtuser01", "Passw0rd!");
		String librarian = bearer("librarian", LIBRARIAN_PASSWORD);

		String body = mockMvc.perform(post("/api/loan-management").header("Authorization", librarian)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"mgmtuser01\",\"bookId\":14}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("mgmtuser01"))
				.andReturn().getResponse().getContentAsString();
		long loanId = objectMapper.readValue(body, JsonNode.class).get("id").asLong();

		mockMvc.perform(get("/api/loan-management").param("keyword", "mgmtuser01").header("Authorization", librarian))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(post("/api/loan-management/" + loanId + "/return").header("Authorization", librarian))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RETURNED"));
		mockMvc.perform(post("/api/loan-management/" + loanId + "/return").header("Authorization", librarian))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ALREADY_RETURNED"));
	}

	@Test
	@DisplayName("상태 필터로 대출 중 목록만 조회한다")
	void filterByStatus() throws Exception {
		mockMvc.perform(get("/api/loan-management").param("status", "RETURNED")
						.header("Authorization", bearer("librarian", LIBRARIAN_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[?(@.status != 'RETURNED')]").isEmpty());
	}

	@Test
	@DisplayName("일반 회원은 대출 관리 API 를 호출할 수 없다(403)")
	void memberForbidden() throws Exception {
		mockMvc.perform(get("/api/loan-management").header("Authorization", bearer("member", MEMBER_PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}
}
