package com.example.library.user.api;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MeIntegrationTest extends IntegrationTestSupport {

	@Test
	@DisplayName("GET /api/me 는 로그인만 되어 있으면 역할과 무관하게 호출된다")
	void me() throws Exception {
		mockMvc.perform(get("/api/me").header("Authorization", bearer("member", MEMBER_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("member"))
				.andExpect(jsonPath("$.roleCode").value("MEMBER"));
	}

	@Test
	@DisplayName("일반 회원 권한: 등록 패턴 문자열 그대로의 URL 목록과 READ 보유 메뉴만 받는다")
	void memberPermissions() throws Exception {
		mockMvc.perform(get("/api/me/permissions").header("Authorization", bearer("member", MEMBER_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.urls", hasItems("GET /api/books", "GET /api/books/{id}", "POST /api/loans",
						"POST /api/loans/{id}/return")))
				.andExpect(jsonPath("$.urls", not(hasItem("DELETE /api/books/{id}"))))
				.andExpect(jsonPath("$.menus").value(contains("BOOK", "MY_LOAN")));
	}

	@Test
	@DisplayName("관리자는 모든 메뉴를 받는다")
	void adminPermissions() throws Exception {
		mockMvc.perform(get("/api/me/permissions").header("Authorization", bearer("admin", ADMIN_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.menus.length()").value(9))
				.andExpect(jsonPath("$.urls", hasItem("PUT /api/admin/roles/{id}/actions")));
	}
}
