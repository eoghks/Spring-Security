package com.example.library.authz.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.authz.domain.RoleRepository;
import com.example.library.support.IntegrationTestSupport;
import com.example.library.user.domain.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 권한 관리 기능으로 스스로 권한을 올리는 경로(역할 권한 저장·액션 URL 편집·역할 변경)와 마지막 관리자 보호를 검증한다.
 * 사서 역할에 관리 권한을 일부 위임한 상황을 만들고, 테스트가 끝나면 사서·회원 역할 권한을 원래대로 되돌린다.
 */
class PrivilegeEscalationIntegrationTest extends IntegrationTestSupport {

	private static final String PASSWORD = "Passw0rd!";

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private List<Long> librarianOriginal;
	private List<Long> memberOriginal;

	@BeforeEach
	void rememberRoles() {
		librarianOriginal = roleActionIds("LIBRARIAN");
		memberOriginal = roleActionIds("MEMBER");
	}

	@AfterEach
	void restoreRoles() throws Exception {
		saveRoleActions(admin(), roleId("LIBRARIAN"), librarianOriginal).andExpect(status().isOk());
		saveRoleActions(admin(), roleId("MEMBER"), memberOriginal).andExpect(status().isOk());
	}

	@Test
	@DisplayName("위임받은 사서는 자기 역할을 편집할 수 없고, 보유하지 않은 액션은 다른 역할에 부여할 수 없다(회수는 가능)")
	void roleGrantGuards() throws Exception {
		delegateToLibrarian(action("ROLE_MANAGE", "READ"), action("ROLE_MANAGE", "GRANT"));
		String librarian = bearer("librarian", LIBRARIAN_PASSWORD);

		expectCode(saveRoleActions(librarian, roleId("LIBRARIAN"), roleActionIds("LIBRARIAN")), 403,
				"CANNOT_EDIT_OWN_ROLE");
		expectCode(saveRoleActions(librarian, roleId("MEMBER"), plus(memberOriginal, action("API_KEY", "ISSUE"))), 403,
				"ACTION_NOT_OWNED");
		saveRoleActions(librarian, roleId("MEMBER"), plus(memberOriginal, action("DASHBOARD", "READ")))
				.andExpect(status().isOk());
		List<Long> revoked = new ArrayList<>(memberOriginal);
		revoked.remove(action("BOOK", "BORROW"));
		saveRoleActions(librarian, roleId("MEMBER"), revoked).andExpect(status().isOk());
	}

	@Test
	@DisplayName("위임받은 사서는 자기 역할이 가진 액션의 URL 을 추가·삭제할 수 없고, 그 밖의 액션은 편집할 수 있다")
	void urlEditGuards() throws Exception {
		delegateToLibrarian(action("ROLE_MANAGE", "READ"), action("ROLE_MANAGE", "URL_EDIT"));
		String librarian = bearer("librarian", LIBRARIAN_PASSWORD);

		expectCode(addUrl(librarian, action("LOAN_MANAGE", "READ"), "/api/admin/users"), 403, "CANNOT_EDIT_OWN_ROLE");
		long ownUrlId = jdbcTemplate.queryForObject("SELECT u.id FROM action_urls u JOIN menu_actions a ON a.id = u.action_id "
				+ "JOIN menus m ON m.id = a.menu_id WHERE m.code = 'LOAN_MANAGE' AND a.code = 'READ' AND u.url_pattern = ?",
				Long.class, "/api/loan-management");
		expectCode(mockMvc.perform(delete("/api/admin/action-urls/" + ownUrlId).header("Authorization", librarian)), 403,
				"CANNOT_EDIT_OWN_ROLE");

		String created = addUrl(librarian, action("BOOK", "READ"), "/api/admin/api-keys")
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		long urlId = objectMapper.readValue(created, JsonNode.class).get("id").asLong();
		mockMvc.perform(delete("/api/admin/action-urls/" + urlId).header("Authorization", librarian))
				.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("역할 변경은 대상의 현재·새 역할 권한을 행위자가 모두 가져야 하고, 마지막 관리자의 역할은 바꿀 수 없다")
	void changeRoleGuards() throws Exception {
		signup("privuser01", PASSWORD);
		signup("privuser02", PASSWORD);
		delegateToLibrarian(action("USER_MANAGE", "READ"), action("USER_MANAGE", "CHANGE_ROLE"));
		String librarian = bearer("librarian", LIBRARIAN_PASSWORD);
		expectCode(changeRole(librarian, "privuser01", "ADMIN"), 403, "ACTION_NOT_OWNED");
		expectCode(changeRole(librarian, "privuser01", "LIBRARIAN"), 403, "ACTION_NOT_OWNED");

		// 모든 액션을 위임받아도 마지막 활성 관리자의 역할은 내릴 수 없다
		saveRoleActions(admin(), roleId("LIBRARIAN"), allActionIds()).andExpect(status().isOk());
		expectCode(changeRole(librarian, "admin", "MEMBER"), 409, "LAST_ADMIN_PROTECTED");

		// 관리자가 한 명 더 있으면 그중 하나는 내릴 수 있다
		changeRole(admin(), "privuser02", "ADMIN").andExpect(status().isOk());
		changeRole(librarian, "privuser02", "MEMBER").andExpect(status().isOk());
	}

	@Test
	@DisplayName("로그인 실패가 한도에 닿아도 마지막 활성 관리자는 잠기지 않는다(다른 관리자는 잠긴다)")
	void lastActiveAdminIsNotLocked() throws Exception {
		signup("privadmin01", PASSWORD);
		changeRole(admin(), "privadmin01", "ADMIN").andExpect(status().isOk());

		failLogins("privadmin01", 5);
		assertThat(userRepository.findByUsername("privadmin01").orElseThrow().isLocked()).isTrue();

		failLogins("admin", 5);
		assertThat(userRepository.findByUsername("admin").orElseThrow().isLocked()).isFalse();
		login("admin", ADMIN_PASSWORD);
	}

	private void delegateToLibrarian(Long... actionIds) throws Exception {
		List<Long> delegated = new ArrayList<>(librarianOriginal);
		delegated.addAll(List.of(actionIds));
		saveRoleActions(admin(), roleId("LIBRARIAN"), delegated).andExpect(status().isOk());
	}

	private void failLogins(String username, int times) throws Exception {
		for (int i = 0; i < times; i++) {
			mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
					.content("{\"username\":\"%s\",\"password\":\"wrong-password\"}".formatted(username)));
		}
	}

	private void expectCode(ResultActions actions, int status, String code) throws Exception {
		actions.andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code));
	}

	private ResultActions saveRoleActions(String token, long roleId, List<Long> actionIds) throws Exception {
		return mockMvc.perform(put("/api/admin/roles/" + roleId + "/actions").header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new RoleActionsRequest(actionIds))));
	}

	private ResultActions addUrl(String token, long actionId, String pattern) throws Exception {
		return mockMvc.perform(post("/api/admin/actions/" + actionId + "/urls").header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"httpMethod\":\"GET\",\"urlPattern\":\"%s\"}".formatted(pattern)));
	}

	private ResultActions changeRole(String token, String username, String roleCode) throws Exception {
		long userId = userRepository.findByUsername(username).orElseThrow().getId();
		return mockMvc.perform(put("/api/admin/users/" + userId + "/role").header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON).content("{\"roleId\":" + roleId(roleCode) + "}"));
	}

	private List<Long> plus(List<Long> base, long actionId) {
		List<Long> result = new ArrayList<>(base);
		result.add(actionId);
		return result;
	}

	private List<Long> roleActionIds(String roleCode) {
		return jdbcTemplate.queryForList("SELECT ra.action_id FROM role_actions ra JOIN roles r ON r.id = ra.role_id "
				+ "WHERE r.code = ? ORDER BY ra.action_id", Long.class, roleCode);
	}

	private List<Long> allActionIds() {
		return jdbcTemplate.queryForList("SELECT id FROM menu_actions ORDER BY id", Long.class);
	}

	private long roleId(String code) {
		return roleRepository.findByCode(code).orElseThrow().getId();
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
