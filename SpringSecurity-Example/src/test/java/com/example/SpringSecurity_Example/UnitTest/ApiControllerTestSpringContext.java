package com.example.SpringSecurity_Example.UnitTest;


import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import com.example.SpringSecurity_Example.controller.ApiController;
import com.example.SpringSecurity_Example.model.constant.MessageEnum;
import com.example.SpringSecurity_Example.model.dto.MemberDto;
import com.example.SpringSecurity_Example.model.vo.SingUpResultVo;
import com.example.SpringSecurity_Example.service.MemberService;
import com.google.gson.Gson;

@WebMvcTest(ApiController.class)
//@Import(SecurityConfig.class) //나는 사용 불가.. Spring Security에 쓰는 당양한 빈들 역시 Import 시켜줘야한다. 예씨 코드로 나둠
@WithMockUser
public class ApiControllerTestSpringContext {
	@MockBean
	private MemberService memberServie;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext context;

	@BeforeEach
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
			.addFilters(new CharacterEncodingFilter("UTF-8", true))
			.build();
	}

	@Test
	@DisplayName("회원 가입 성공 Test <Spring Context 사용>")
	void signUpSuccess() throws Exception {
		//given -> 테스트 데이터 생성
		MemberDto request = getSignUpRequest();
		SingUpResultVo expectResponse = getSignUpResponse();

		doNothing().when(memberServie).createUser(any());

		//when -> 수행
		ResultActions resultActions = mockMvc.perform(
				MockMvcRequestBuilders.post("/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(new Gson().toJson(request))
				.with(csrf()));

		//then -> 결과 비교
		MvcResult mvcResult = resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("loginId").exists())
				.andExpect(jsonPath("msg").exists())
				.andExpect(jsonPath("msg", is(expectResponse.getMsg())))
				.andDo(print())
				.andReturn();
		System.out.println("mvcResult : " + mvcResult.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("로그인 성공 Test <Spring Context 사용>")
	void LoginSuccess() throws Exception {
		//given -> 테스트 데이터 생성
		MemberDto request = getSignUpRequest();
		String response = "Login Success";

		given(memberServie.login(any())).willReturn(response);

		//when -> 수행
		ResultActions resultActions = mockMvc.perform(
				MockMvcRequestBuilders.post("/login")
				.content(new Gson().toJson(request))
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf()));

		//then -> 결과 비교
		MvcResult mvcResult = resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("loginId").exists())
				.andDo(print())
				.andReturn();

		System.out.println("mvcResult : " + mvcResult.getResponse().getContentAsString());
	}

	private MemberDto getSignUpRequest() {
		MemberDto dto = new MemberDto();
		dto.setLoginId("unitTest2");
		dto.setPassword("unitTest2");
		return dto;
	}

	private SingUpResultVo getSignUpResponse() {
		return new SingUpResultVo("unitTest", MessageEnum.SignUpSuccess.getMsg());
	}
}
