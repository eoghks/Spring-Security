package com.example.library.authz.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.authz.domain.RoleRepository;
import com.example.library.support.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

class AuthzAdminIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("역할에 액션을 부여·회수하면 기존 토큰에도 즉시 반영된다(403 → 200 → 403)")
	void grantAndRevokeAppliesImmediately() throws Exception {
		String member = bearer("member", MEMBER_PASSWORD);
		long memberRoleId = roleRepository.findByCode("MEMBER").orElseThrow().getId();
		List<Long> original = currentActionIds(memberRoleId);
		mockMvc.perform(get("/api/dashboard/stats").header("Authorization", member)).andExpect(status().isForbidden());

		List<Long> granted = new ArrayList<>(original);
		granted.add(actionId("DASHBOARD", "READ"));
		saveRoleActions(memberRoleId, granted).andExpect(status().isOk());
		mockMvc.perform(get("/api/dashboard/stats").header("Authorization", member)).andExpect(status().isOk());
		mockMvc.perform(get("/api/me/permissions").header("Authorization", member))
				.andExpect(jsonPath("$.menus[0]").value("DASHBOARD"));

		saveRoleActions(memberRoleId, original).andExpect(status().isOk());
		mockMvc.perform(get("/api/dashboard/stats").header("Authorization", member)).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("액션에 URL 을 추가·삭제하면 규칙이 즉시 다시 적재된다")
	void urlMappingReload() throws Exception {
		String librarian = bearer("librarian", LIBRARIAN_PASSWORD);
		mockMvc.perform(get("/api/admin/users").header("Authorization", librarian)).andExpect(status().isForbidden());

		String created = mockMvc.perform(post("/api/admin/actions/" + actionId("LOAN_MANAGE", "READ") + "/urls")
						.header("Authorization", admin()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"httpMethod\":\"GET\",\"urlPattern\":\"/api/admin/users\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long urlId = objectMapper.readValue(created, JsonNode.class).get("id").asLong();
		mockMvc.perform(get("/api/admin/users").header("Authorization", librarian)).andExpect(status().isOk());

		mockMvc.perform(delete("/api/admin/action-urls/" + urlId).header("Authorization", admin()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/admin/users").header("Authorization", librarian)).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("관리자 역할은 편집할 수 없고, 잘못된 URL 패턴은 400")
	void guards() throws Exception {
		long adminRoleId = roleRepository.findByCode("ADMIN").orElseThrow().getId();
		saveRoleActions(adminRoleId, List.of())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("SYSTEM_ROLE_PROTECTED"));

		mockMvc.perform(post("/api/admin/actions/" + actionId("BOOK", "READ") + "/urls")
						.header("Authorization", admin()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"httpMethod\":\"GET\",\"urlPattern\":\"/api/**/books\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("메뉴 트리는 메뉴 → 액션 → URL 구조로 온다")
	void menuTree() throws Exception {
		mockMvc.perform(get("/api/admin/menus").header("Authorization", admin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(9))
				.andExpect(jsonPath("$[0].code").value("DASHBOARD"))
				.andExpect(jsonPath("$[0].actions[0].authorityCode").value("DASHBOARD:READ"))
				.andExpect(jsonPath("$[0].actions[0].urls[0].urlPattern").value("/api/dashboard/stats"));
	}

	private ResultActions saveRoleActions(long roleId, List<Long> actionIds) throws Exception {
		return mockMvc.perform(put("/api/admin/roles/" + roleId + "/actions").header("Authorization", admin())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new RoleActionsRequest(actionIds))));
	}

	private List<Long> currentActionIds(long roleId) throws Exception {
		String body = mockMvc.perform(get("/api/admin/roles/" + roleId + "/actions").header("Authorization", admin()))
				.andReturn().getResponse().getContentAsString();
		List<Long> ids = new ArrayList<>();
		objectMapper.readValue(body, JsonNode.class).get("actionIds").forEach(node -> ids.add(node.asLong()));
		return ids;
	}

	private long actionId(String menuCode, String actionCode) {
		return jdbcTemplate.queryForObject(
				"SELECT a.id FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = ? AND a.code = ?",
				Long.class, menuCode, actionCode);
	}

	private String admin() throws Exception {
		return bearer("admin", ADMIN_PASSWORD);
	}
}
