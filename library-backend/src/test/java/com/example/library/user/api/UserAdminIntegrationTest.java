package com.example.library.user.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.authz.domain.RoleRepository;
import com.example.library.support.IntegrationTestSupport;
import com.example.library.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class UserAdminIntegrationTest extends IntegrationTestSupport {

	private static final String PASSWORD = "Passw0rd!";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Test
	@DisplayName("역할을 사서로 바꾸면 이미 발급된 토큰으로도 다음 요청부터 사서 권한이 적용된다")
	void changeRoleAppliesImmediately() throws Exception {
		signup("roleuser01", PASSWORD);
		String user = bearer("roleuser01", PASSWORD);
		mockMvc.perform(get("/api/loan-management").header("Authorization", user)).andExpect(status().isForbidden());

		changeRole("roleuser01", "LIBRARIAN").andExpect(status().isOk())
				.andExpect(jsonPath("$.roleCode").value("LIBRARIAN"));

		mockMvc.perform(get("/api/loan-management").header("Authorization", user)).andExpect(status().isOk());
	}

	@Test
	@DisplayName("잠긴 계정의 기존 토큰은 401 ACCOUNT_LOCKED, 관리자가 잠금 해제하면 다시 로그인된다")
	void unlock() throws Exception {
		signup("lockuser01", PASSWORD);
		String token = bearer("lockuser01", PASSWORD);
		for (int i = 0; i < 5; i++) {
			mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\":\"lockuser01\",\"password\":\"bad\"}"));
		}
		mockMvc.perform(get("/api/me").header("Authorization", token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));

		long id = userRepository.findByUsername("lockuser01").orElseThrow().getId();
		mockMvc.perform(post("/api/admin/users/" + id + "/unlock").header("Authorization", admin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.locked").value(false));

		login("lockuser01", PASSWORD);
	}

	@Test
	@DisplayName("자기 자신의 역할은 바꿀 수 없고, 사서는 회원 관리 API 를 호출할 수 없다")
	void guards() throws Exception {
		changeRole("admin", "MEMBER")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("CANNOT_CHANGE_OWN_ROLE"));
		mockMvc.perform(get("/api/admin/users").header("Authorization", bearer("librarian", LIBRARIAN_PASSWORD)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/admin/roles").header("Authorization", admin()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3));
	}

	private ResultActions changeRole(String username, String roleCode)
			throws Exception {
		long userId = userRepository.findByUsername(username).orElseThrow().getId();
		long roleId = roleRepository.findByCode(roleCode).orElseThrow().getId();
		return mockMvc.perform(put("/api/admin/users/" + userId + "/role").header("Authorization", admin())
				.contentType(MediaType.APPLICATION_JSON).content("{\"roleId\":" + roleId + "}"));
	}

	private String admin() throws Exception {
		return bearer("admin", ADMIN_PASSWORD);
	}
}
