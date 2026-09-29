package com.example.library.apikey.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.authz.domain.RoleRepository;
import com.example.library.support.IntegrationTestSupport;
import com.example.library.user.domain.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * API Key 권한이 발급 시점이 아니라 발급자의 "현재" 권한·잠금·접속 조건에 묶이는지 검증한다.
 */
class ApiKeyOwnerBindingIntegrationTest extends IntegrationTestSupport {

	private static final String PASSWORD = "Passw0rd!";
	private static final String OWNER = "keyowner01";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Test
	@DisplayName("발급자가 강등되면 키의 권한도 발급자 현재 역할 이내로 줄고, 발급자 접속 조건·잠금도 키에 적용된다")
	void keyFollowsOwner() throws Exception {
		signup(OWNER, PASSWORD);
		changeRole(OWNER, "ADMIN");
		String owner = bearer(OWNER, PASSWORD);
		String dashboardKey = issueKey(owner, action("DASHBOARD", "READ"));
		String bookKey = issueKey(owner, action("BOOK", "READ"));
		mockMvc.perform(get("/api/dashboard/stats").header("X-API-KEY", dashboardKey)).andExpect(status().isOk());

		// 일반 회원으로 강등 — 회원에게 없는 대시보드 권한은 키에서도 사라지고, 회원도 가진 도서 조회는 남는다
		changeRole(OWNER, "MEMBER");
		mockMvc.perform(get("/api/dashboard/stats").header("X-API-KEY", dashboardKey))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
		mockMvc.perform(get("/api/books").header("X-API-KEY", bookKey)).andExpect(status().isOk());

		// 발급자 접속 조건(허용 IP)이 키 호출에도 적용된다
		saveOwnerAllowedIp("10.0.0.0/8");
		mockMvc.perform(get("/api/books").header("X-API-KEY", bookKey))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_CONDITION_DENIED"));
		mockMvc.perform(delete("/api/admin/access-conditions/" + ownerId()).header("Authorization", admin()))
				.andExpect(status().isNoContent());

		// 발급자가 잠기면 키도 즉시 쓸 수 없다
		for (int i = 0; i < 5; i++) {
			mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\":\"%s\",\"password\":\"bad\"}".formatted(OWNER)));
		}
		mockMvc.perform(get("/api/books").header("X-API-KEY", bookKey))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_API_KEY"));
	}

	private void saveOwnerAllowedIp(String cidr) throws Exception {
		mockMvc.perform(put("/api/admin/access-conditions/" + ownerId()).header("Authorization", admin())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"allowedIps\":[\"%s\"],\"allowedDays\":[]}".formatted(cidr)))
				.andExpect(status().isOk());
	}

	private String issueKey(String token, long actionId) throws Exception {
		String response = mockMvc.perform(post("/api/admin/api-keys").header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"발급자 묶음\",\"actionIds\":[%d],\"allowedIps\":[]}".formatted(actionId)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readValue(response, JsonNode.class).get("apiKey").asText();
	}

	private void changeRole(String username, String roleCode) throws Exception {
		long userId = userRepository.findByUsername(username).orElseThrow().getId();
		long roleId = roleRepository.findByCode(roleCode).orElseThrow().getId();
		mockMvc.perform(put("/api/admin/users/" + userId + "/role").header("Authorization", admin())
						.contentType(MediaType.APPLICATION_JSON).content("{\"roleId\":" + roleId + "}"))
				.andExpect(status().isOk());
	}

	private long ownerId() {
		return userRepository.findByUsername(OWNER).orElseThrow().getId();
	}

	private long action(String menuCode, String actionCode) {
		return jdbcTemplate.queryForObject(
				"SELECT a.id FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = ? AND a.code = ?",
				Long.class, menuCode, actionCode);
	}

	private String admin() throws Exception {
		return bearer("admin", ADMIN_PASSWORD);
	}
}
