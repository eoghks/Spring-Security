package com.example.library.access.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.access.domain.UserAccessCondition.AccessDays;
import com.example.library.auth.api.TokenResponse;
import com.example.library.support.IntegrationTestSupport;
import com.example.library.user.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class AccessConditionIntegrationTest extends IntegrationTestSupport {

	private static final String PASSWORD = "Passw0rd!";
	private static final String ALLOWED_IP = "10.20.30.40";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private Clock clock;

	@Test
	@DisplayName("허용 IP 밖에서 호출하면 403 ACCESS_CONDITION_DENIED, 조건 삭제 즉시 다시 허용된다")
	void ipCondition() throws Exception {
		signup("acuser01", PASSWORD);
		String user = bearer("acuser01", PASSWORD);
		long userId = userId("acuser01");

		saveCondition(userId, "{\"allowedIps\":[\"10.0.0.0/8\"],\"allowedDays\":[]}").andExpect(status().isOk());

		mockMvc.perform(get("/api/books").header("Authorization", user))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_CONDITION_DENIED"));
		mockMvc.perform(get("/api/books").header("Authorization", user).with(request -> {
					request.setRemoteAddr("10.20.30.40");
					return request;
				}))
				.andExpect(status().isOk());

		mockMvc.perform(delete("/api/admin/access-conditions/" + userId).header("Authorization", admin()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/books").header("Authorization", user)).andExpect(status().isOk());
	}

	@Test
	@DisplayName("오늘이 허용 요일이 아니거나 허용 기간이 지났으면 403")
	void dayAndPeriodCondition() throws Exception {
		signup("acuser02", PASSWORD);
		String user = bearer("acuser02", PASSWORD);
		long userId = userId("acuser02");
		String otherDay = AccessDays.code(LocalDate.now(clock).getDayOfWeek().plus(1));

		saveCondition(userId, "{\"allowedIps\":[],\"allowedDays\":[\"" + otherDay + "\"]}").andExpect(status().isOk());
		mockMvc.perform(get("/api/me").header("Authorization", user))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_CONDITION_DENIED"));

		String yesterday = LocalDate.now(clock).minusDays(1).toString();
		saveCondition(userId, "{\"allowedIps\":[],\"allowedDays\":[],\"validTo\":\"" + yesterday + "\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.validTo").value(yesterday));
		mockMvc.perform(get("/api/me").header("Authorization", user)).andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("비밀번호가 맞아도 허용 IP 밖이면 로그인 403 — 실패 횟수는 오르지 않아 반복해도 잠기지 않는다")
	void loginDeniedOutsideCondition() throws Exception {
		signup("acuser03", PASSWORD);
		long userId = userId("acuser03");
		saveCondition(userId, "{\"allowedIps\":[\"10.0.0.0/8\"],\"allowedDays\":[]}").andExpect(status().isOk());

		for (int i = 0; i < 6; i++) {
			postLogin("acuser03", "127.0.0.1")
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.code").value("ACCESS_CONDITION_DENIED"));
		}
		assertThat(userRepository.findById(userId).orElseThrow().getFailedLoginCount()).isZero();

		postLogin("acuser03", ALLOWED_IP).andExpect(status().isOk());
	}

	@Test
	@DisplayName("토큰 재발급도 허용 IP 밖이면 403 ACCESS_CONDITION_DENIED")
	void refreshDeniedOutsideCondition() throws Exception {
		signup("acuser04", PASSWORD);
		long userId = userId("acuser04");
		saveCondition(userId, "{\"allowedIps\":[\"10.0.0.0/8\"],\"allowedDays\":[]}").andExpect(status().isOk());
		TokenResponse tokens = readTokens(postLogin("acuser04", ALLOWED_IP).andExpect(status().isOk()));

		postRefresh(tokens.refreshToken(), "127.0.0.1")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_CONDITION_DENIED"));

		TokenResponse again = readTokens(postLogin("acuser04", ALLOWED_IP).andExpect(status().isOk()));
		postRefresh(again.refreshToken(), ALLOWED_IP).andExpect(status().isOk());
	}

	@Test
	@DisplayName("IP 형식·요일 형식·기간 역전은 400")
	void validation() throws Exception {
		long userId = userId("member");

		saveCondition(userId, "{\"allowedIps\":[\"example.com\"],\"allowedDays\":[\"MONDAY\"]}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.length()").value(2));
		saveCondition(userId, """
				{"allowedIps":[],"allowedDays":[],"validFrom":"2026-05-01","validTo":"2026-04-01"}
				""")
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("일반 회원은 접속 조건을 관리할 수 없다(403)")
	void memberForbidden() throws Exception {
		mockMvc.perform(get("/api/admin/access-conditions").header("Authorization", bearer("member", MEMBER_PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	private ResultActions postLogin(String username, String remoteAddr) throws Exception {
		String body = "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD);
		return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)
				.with(request -> {
					request.setRemoteAddr(remoteAddr);
					return request;
				}));
	}

	private ResultActions postRefresh(String refreshToken, String remoteAddr) throws Exception {
		String body = "{\"refreshToken\":\"%s\"}".formatted(refreshToken);
		return mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body)
				.with(request -> {
					request.setRemoteAddr(remoteAddr);
					return request;
				}));
	}

	private TokenResponse readTokens(ResultActions result) throws Exception {
		return objectMapper.readValue(result.andReturn().getResponse().getContentAsString(), TokenResponse.class);
	}

	private ResultActions saveCondition(long userId, String body) throws Exception {
		return mockMvc.perform(put("/api/admin/access-conditions/" + userId).header("Authorization", admin())
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private String admin() throws Exception {
		return bearer("admin", ADMIN_PASSWORD);
	}

	private long userId(String username) {
		return userRepository.findByUsername(username).orElseThrow().getId();
	}
}
