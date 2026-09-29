package com.example.library.common.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.support.Concurrently;
import com.example.library.support.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * 사전 중복 검사를 동시에 통과한 요청이 유니크 제약에 걸려도 500 이 아니라 409 로 응답하는지 검증한다.
 */
class UniqueConstraintRaceIntegrationTest extends IntegrationTestSupport {

	private static final int ROUNDS = 3;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("같은 ISBN 도서를 동시에 등록하면 하나만 201, 나머지는 409 DUPLICATE_ISBN")
	void concurrentBookCreateWithSameIsbn() throws Exception {
		String admin = bearer("admin", ADMIN_PASSWORD);
		for (int round = 0; round < ROUNDS; round++) {
			String body = """
					{"isbn":"978000000040%d","title":"경합 등록","author":"저자","publisher":"출판사","category":"IT","totalQuantity":1}
					""".formatted(round);

			List<MockHttpServletResponse> responses = Concurrently.run(List.of(
					() -> postJson("/api/books", body, admin),
					() -> postJson("/api/books", body, admin)));

			assertThat(responses).extracting(MockHttpServletResponse::getStatus).containsExactlyInAnyOrder(201, 409);
			assertThat(codeOf(conflictOf(responses))).isEqualTo("DUPLICATE_ISBN");
		}
	}

	@Test
	@DisplayName("같은 액션 URL 을 동시에 추가하면 하나만 201, 나머지는 409 DUPLICATE_ACTION_URL")
	void concurrentActionUrlAdd() throws Exception {
		String admin = bearer("admin", ADMIN_PASSWORD);
		long actionId = jdbcTemplate.queryForObject(
				"SELECT a.id FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'BOOK' AND a.code = 'READ'",
				Long.class);
		String url = "/api/admin/actions/" + actionId + "/urls";
		String body = "{\"httpMethod\":\"GET\",\"urlPattern\":\"/api/admin/api-keys\"}";
		for (int round = 0; round < ROUNDS; round++) {
			List<MockHttpServletResponse> responses = Concurrently.run(List.of(
					() -> postJson(url, body, admin),
					() -> postJson(url, body, admin)));

			assertThat(responses).extracting(MockHttpServletResponse::getStatus).containsExactlyInAnyOrder(201, 409);
			assertThat(codeOf(conflictOf(responses))).isEqualTo("DUPLICATE_ACTION_URL");
			removeCreatedUrl(responses, admin);
		}
	}

	private void removeCreatedUrl(List<MockHttpServletResponse> responses, String admin) throws Exception {
		MockHttpServletResponse created = responses.stream().filter(r -> r.getStatus() == 201).findFirst().orElseThrow();
		long urlId = objectMapper.readValue(created.getContentAsString(), JsonNode.class).get("id").asLong();
		mockMvc.perform(delete("/api/admin/action-urls/" + urlId).header("Authorization", admin))
				.andExpect(status().isNoContent());
	}

	private MockHttpServletResponse conflictOf(List<MockHttpServletResponse> responses) {
		return responses.stream().filter(r -> r.getStatus() == 409).findFirst().orElseThrow();
	}

	private String codeOf(MockHttpServletResponse response) throws Exception {
		return objectMapper.readValue(response.getContentAsString(), JsonNode.class).get("code").asText();
	}

	private MockHttpServletResponse postJson(String url, String body, String token) throws Exception {
		return mockMvc.perform(post(url).header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andReturn().getResponse();
	}
}
