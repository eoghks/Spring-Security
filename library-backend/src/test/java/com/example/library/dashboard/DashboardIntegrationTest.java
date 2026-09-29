package com.example.library.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DashboardIntegrationTest extends IntegrationTestSupport {

	@Test
	@DisplayName("사서·관리자는 대시보드 통계를 보고, 일반 회원은 403")
	void stats() throws Exception {
		mockMvc.perform(get("/api/dashboard/stats").header("Authorization", bearer("librarian", LIBRARIAN_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titles").isNumber())
				.andExpect(jsonPath("$.activeLoans").isNumber());
		mockMvc.perform(get("/api/dashboard/stats").header("Authorization", bearer("member", MEMBER_PASSWORD)))
				.andExpect(status().isForbidden());
	}
}
