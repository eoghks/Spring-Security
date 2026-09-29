package com.example.library.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.auth.api.TokenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 통합 테스트 공통 설정.
 * 모든 통합 테스트가 같은 설정을 공유해 스프링 컨텍스트(와 Hazelcast 멤버)를 하나만 띄운다.
 * 클러스터 이름을 매번 무작위로 정해 로컬에서 실행 중인 개발 서버 클러스터에 합류하지 않게 한다.
 */
@SpringBootTest(properties = {
		"app.hazelcast.cluster-name=library-test-${random.uuid}",
		"app.hazelcast.port=5901",
		// 여러 테스트가 같은 IP(127.0.0.1)로 로그인 실패를 쌓으므로 IP 단위 제한은 사실상 끈다(전용 테스트에서 따로 검증)
		"app.security.login.max-failures-per-minute=1000"
})
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

	/** 시드 계정 비밀번호(개발용) */
	protected static final String ADMIN_PASSWORD = "Admin123!";
	protected static final String LIBRARIAN_PASSWORD = "Librarian123!";
	protected static final String MEMBER_PASSWORD = "Member123!";

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	/** 로그인해 토큰을 받는다 */
	protected TokenResponse login(String username, String password) throws Exception {
		String body = objectMapper.writeValueAsString(new LoginBody(username, password));
		String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readValue(response, TokenResponse.class);
	}

	/** 로그인 후 "Bearer ..." 헤더 값을 돌려준다 */
	protected String bearer(String username, String password) throws Exception {
		return "Bearer " + login(username, password).accessToken();
	}

	/** 새 일반 회원을 가입시킨다 */
	protected void signup(String username, String password) throws Exception {
		String body = """
				{"username":"%s","password":"%s","name":"테스트","email":"%s@library.local"}
				""".formatted(username, password, username);
		mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
	}

	private record LoginBody(String username, String password) {
	}
}
