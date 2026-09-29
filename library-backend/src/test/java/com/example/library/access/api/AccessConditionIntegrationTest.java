package com.example.library.access.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.access.domain.UserAccessCondition.AccessDays;
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
