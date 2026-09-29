package com.example.library.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.common.error.BusinessException;
import com.example.library.support.IntegrationTestSupport;
import com.example.library.user.domain.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * "본인" 이 필요한 API 를 API Key 로 부르면 500(주체 불일치)이 아니라 403 USER_ONLY 인지 검증한다.
 */
class UserOnlyEndpointIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private UserRepository userRepository;

	@Test
	@DisplayName("URL 인가를 통과하는 액션을 가진 API Key 라도 사용자 전용 API 는 403 USER_ONLY")
	void apiKeyCannotCallUserOnlyEndpoints() throws Exception {
		String key = issueKey(List.of(action("BOOK", "BORROW"), action("MY_LOAN", "READ"), action("MY_LOAN", "RETURN"),
				action("API_KEY", "ISSUE"), action("USER_MANAGE", "CHANGE_ROLE")));
		long memberId = userRepository.findByUsername("member").orElseThrow().getId();

		expectUserOnly(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content("{\"bookId\":1}"), key);
		expectUserOnly(get("/api/loans/me"), key);
		expectUserOnly(post("/api/loans/1/return"), key);
		expectUserOnly(post("/api/admin/api-keys").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"키로 키 발급\",\"actionIds\":[%d],\"allowedIps\":[]}".formatted(action("BOOK", "READ"))), key);
		expectUserOnly(put("/api/admin/users/" + memberId + "/role").contentType(MediaType.APPLICATION_JSON)
				.content("{\"roleId\":1}"), key);
	}

	@Test
	@DisplayName("UserPrincipal.require 는 사용자 주체만 통과시킨다")
	void requireAcceptsOnlyUsers() {
		UserPrincipal user = new UserPrincipal(1L, "member", 3L);

		assertThat(UserPrincipal.require(user)).isSameAs(user);
		assertThatThrownBy(() -> UserPrincipal.require(new ApiKeyPrincipal(1L, "key", Set.of(), Set.of(), 1L, 1L)))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("API Key");
	}

	private void expectUserOnly(MockHttpServletRequestBuilder request, String key) throws Exception {
		mockMvc.perform(request.header("X-API-KEY", key))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("USER_ONLY"));
	}

	private String issueKey(List<Long> actionIds) throws Exception {
		String response = mockMvc.perform(post("/api/admin/api-keys").header("Authorization", bearer("admin", ADMIN_PASSWORD))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"사용자 전용 검사\",\"actionIds\":%s,\"allowedIps\":[]}".formatted(actionIds)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readValue(response, JsonNode.class).get("apiKey").asText();
	}

	private long action(String menuCode, String actionCode) {
		return jdbcTemplate.queryForObject(
				"SELECT a.id FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = ? AND a.code = ?",
				Long.class, menuCode, actionCode);
	}
}
