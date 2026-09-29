package com.example.library.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.authz.cache.AuthzChangedEvent;
import com.example.library.authz.cache.CacheNames;
import com.example.library.common.crypto.Hashing;
import com.example.library.config.ApiKeyProtectionProperties;
import com.example.library.support.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.hazelcast.core.HazelcastInstance;
import java.time.Clock;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 없는 API Key 음성 캐시와 IP 별 인증 실패 제한(429).
 * 다른 테스트와 실패 횟수가 섞이지 않도록 테스트마다 전용 IP 를 쓴다.
 */
@RecordApplicationEvents
class ApiKeyProtectionIntegrationTest extends IntegrationTestSupport {

	/** data.sql 시드 샘플 키(BOOK:READ) — 개발용 */
	private static final String SAMPLE_KEY = "lib_Elsu1z3_KkwNJYk7v7R7BxLaFBJpJV4qc61GHDjZvNY";
	/** 형식은 맞지만 등록되지 않은 키 */
	private static final String UNKNOWN_KEY = "lib_" + "Z".repeat(43);

	@Autowired
	private HazelcastInstance hazelcast;

	@Autowired
	private ApiKeyProtectionProperties protectionProperties;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Clock clock;

	@Autowired
	private ApplicationEvents events;

	@Test
	@DisplayName("없는 키로 호출하면 401 이고 해당 해시가 음성 캐시에 남는다")
	void unknownKeyIsNegativelyCached() throws Exception {
		callWithKey(UNKNOWN_KEY, "192.0.2.10")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_API_KEY"));

		assertThat(hazelcast.getMap(CacheNames.API_KEY_MISSES).containsKey(Hashing.sha256Hex(UNKNOWN_KEY))).isTrue();
	}

	@Test
	@DisplayName("한 IP 가 1분 한도를 넘게 실패하면 429 — 그 분 동안은 유효한 키도 거절, 다른 IP 는 영향 없음")
	void tooManyFailuresFromOneIp() throws Exception {
		awaitFreshMinute();
		String attacker = "192.0.2.20";
		for (int i = 0; i < protectionProperties.maxFailuresPerMinute(); i++) {
			callWithKey(UNKNOWN_KEY, attacker).andExpect(status().isUnauthorized());
		}

		callWithKey(UNKNOWN_KEY, attacker)
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists(HttpHeaders.RETRY_AFTER))
				.andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
		callWithKey(SAMPLE_KEY, attacker).andExpect(status().isTooManyRequests());

		callWithKey(UNKNOWN_KEY, "192.0.2.21").andExpect(status().isUnauthorized());
		callWithKey(SAMPLE_KEY, "192.0.2.21").andExpect(status().isOk());
	}

	@Test
	@DisplayName("키를 발급하면 그 해시의 캐시 무효화 이벤트가 발행된다(음성 캐시 제거)")
	void issuePublishesEviction() throws Exception {
		String response = mockMvc.perform(post("/api/admin/api-keys")
						.header("Authorization", bearer("admin", ADMIN_PASSWORD))
						.contentType(MediaType.APPLICATION_JSON).content(issueBody()))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String rawKey = objectMapper.readValue(response, JsonNode.class).get("apiKey").asText();

		assertThat(events.stream(AuthzChangedEvent.ApiKeyChanged.class))
				.extracting(AuthzChangedEvent.ApiKeyChanged::keyHash)
				.contains(Hashing.sha256Hex(rawKey));
	}

	private ResultActions callWithKey(String key, String remoteAddr) throws Exception {
		return mockMvc.perform(get("/api/books").header("X-API-KEY", key).with(request -> {
			request.setRemoteAddr(remoteAddr);
			return request;
		}));
	}

	/** 분 경계에서 버킷이 바뀌어 결과가 흔들리지 않도록, 분의 끝 무렵이면 다음 분까지 기다린다 */
	private void awaitFreshMinute() throws InterruptedException {
		int second = LocalTime.now(clock).getSecond();
		if (second >= 50) {
			Thread.sleep((61L - second) * 1000);
		}
	}

	private String issueBody() {
		Long bookRead = jdbcTemplate.queryForObject(
				"SELECT a.id FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = ? AND a.code = ?",
				Long.class, "BOOK", "READ");
		return "{\"name\":\"발급 이벤트 확인\",\"actionIds\":[%d],\"allowedIps\":[]}".formatted(bookRead);
	}
}
