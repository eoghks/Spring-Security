package com.example.SpringSecurity_Example.UnitTest;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.SpringSecurity_Example.controller.ApiController;
import com.example.SpringSecurity_Example.model.constant.MessageEnum;
import com.example.SpringSecurity_Example.model.dto.MemberDto;
import com.example.SpringSecurity_Example.model.vo.SingUpResultVo;
import com.example.SpringSecurity_Example.service.MemberService;
import com.google.gson.Gson;

@ExtendWith(MockitoExtension.class)
public class ApiControllerTestNotthing {
	@InjectMocks
	private ApiController apiCotroller;

	@Mock
	private MemberService memberServie;

	private MockMvc mockMvc;

	@BeforeEach
	public void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(apiCotroller)
				.defaultResponseCharacterEncoding(StandardCharsets.UTF_8).build();
	}

	@Test
	@DisplayName("회원 가입 성공 Test <웹 서버 없이>")
	void signUpSuccess() throws Exception {
		//given -> 테스트 데이터 생성
		MemberDto request = getSignUpRequest();
		SingUpResultVo expectResponse = getSignUpResponse();

		//doNothing().when(memberServie).createUser(request);

		//when -> 수행
		ResultActions resultActions = mockMvc.perform(
				MockMvcRequestBuilders.post("/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(new Gson().toJson(request)));

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

	private MemberDto getSignUpRequest() {
		MemberDto dto = new MemberDto();
		dto.setLoginId("unitTest");
		dto.setPassword("unitTest");
		return dto;
	}

	private SingUpResultVo getSignUpResponse() {
		return new SingUpResultVo("unitTest", MessageEnum.SignUpSuccess.getMsg());
	}
}
