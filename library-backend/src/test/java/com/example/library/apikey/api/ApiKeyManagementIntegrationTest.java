package com.example.library.apikey.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.support.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

class ApiKeyManagementIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("발급한 키는 원문이 한 번만 오고, 폐기 즉시 401 이 된다")
	void issueUseRevoke() throws Exception {
		String admin = bearer("admin", ADMIN_PASSWORD);
		JsonNode issued = issue(admin, "[]");
		String rawKey = issued.get("apiKey").asText();
		long id = issued.get("detail").get("id").asLong();

		mockMvc.perform(get("/api/books").header("X-API-KEY", rawKey)).andExpect(status().isOk());
		mockMvc.perform(get("/api/admin/api-keys").header("Authorization", admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].keyPrefix").value(rawKey.substring(0, 8)))
				.andExpect(jsonPath("$[0].apiKey").doesNotExist());

		mockMvc.perform(post("/api/admin/api-keys/" + id + "/revoke").header("Authorization", admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REVOKED"));
		mockMvc.perform(get("/api/books").header("X-API-KEY", rawKey))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_API_KEY"));
	}

	@Test
	@DisplayName("허용 IP 가 지정된 키를 다른 IP 에서 쓰면 403 ACCESS_CONDITION_DENIED")
	void allowedIps() throws Exception {
		String rawKey = issue(bearer("admin", ADMIN_PASSWORD), "[\"10.0.0.0/8\"]").get("apiKey").asText();

		mockMvc.perform(get("/api/books").header("X-API-KEY", rawKey))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_CONDITION_DENIED"));
		mockMvc.perform(get("/api/books").header("X-API-KEY", rawKey).with(request -> {
					request.setRemoteAddr("10.9.8.7");
					return request;
				}))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("허용 IP 의 앞뒤 공백은 저장 전에 다듬어져, 그 IP 에서 정상 호출된다(500 이 아니다)")
	void allowedIpsAreTrimmed() throws Exception {
		JsonNode issued = issue(bearer("admin", ADMIN_PASSWORD), "[\" 10.9.8.7 \"]");
		long id = issued.get("detail").get("id").asLong();

		mockMvc.perform(get("/api/books").header("X-API-KEY", issued.get("apiKey").asText()).with(request -> {
					request.setRemoteAddr("10.9.8.7");
					return request;
				}))
				.andExpect(status().isOk());
		assertThat(jdbcTemplate.queryForList("SELECT ip FROM api_key_allowed_ips WHERE api_key_id = ?", String.class, id))
				.containsExactly("10.9.8.7");
	}

	@Test
	@DisplayName("사서는 API Key 를 발급할 수 없다(403)")
	void librarianForbidden() throws Exception {
		mockMvc.perform(post("/api/admin/api-keys").header("Authorization", bearer("librarian", LIBRARIAN_PASSWORD))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body("[]")))
				.andExpect(status().isForbidden());
	}

	private JsonNode issue(String admin, String allowedIps) throws Exception {
		String response = mockMvc.perform(post("/api/admin/api-keys").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON).content(body(allowedIps)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readValue(response, JsonNode.class);
	}

	private String body(String allowedIps) {
		Long bookRead = jdbcTemplate.queryForObject(
				"SELECT a.id FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = ? AND a.code = ?",
				Long.class, "BOOK", "READ");
		return "{\"name\":\"테스트 키\",\"actionIds\":[%d],\"allowedIps\":%s}".formatted(bookRead, allowedIps);
	}
}
