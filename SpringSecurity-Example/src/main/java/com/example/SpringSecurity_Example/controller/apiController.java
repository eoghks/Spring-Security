package com.example.SpringSecurity_Example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.SpringSecurity_Example.model.constant.MessageEnum;
import com.example.SpringSecurity_Example.model.dto.MemberDto;
import com.example.SpringSecurity_Example.model.vo.ApiResultVo;
import com.example.SpringSecurity_Example.model.vo.LoginResultVo;
import com.example.SpringSecurity_Example.model.vo.SingUpResultVo;
import com.example.SpringSecurity_Example.service.MemberService;

import jakarta.validation.Valid;

@RestController
public class apiController {
	@Autowired
	private MemberService memberService;

	@PostMapping("/signup")
	public ResponseEntity<SingUpResultVo> signup(@Valid @RequestBody MemberDto memberDto) throws Exception{
		memberService.createUser(memberDto);
		SingUpResultVo result = new SingUpResultVo(memberDto.getLoginId(), MessageEnum.SignUpSuccess.getMsg());
		return new ResponseEntity<>( result, HttpStatus.OK);
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResultVo> login(@Valid @RequestBody MemberDto memberDto) throws Exception {
		String token = memberService.login(memberDto);
		LoginResultVo result = new LoginResultVo(memberDto.getLoginId(), token, MessageEnum.SignUpSuccess.getMsg());
		return ResponseEntity.status(HttpStatus.OK).body(result);
	}

	@PostMapping("/api1")
	public ResponseEntity<ApiResultVo> api1() {
		ApiResultVo result = new ApiResultVo(MessageEnum.ApiSucess.getMsg());
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@PostMapping("/api2")
	public ResponseEntity<ApiResultVo> api2() {
		ApiResultVo result = new ApiResultVo(MessageEnum.ApiSucess.getMsg());
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@PostMapping("/api3")
	public ResponseEntity<ApiResultVo> api3() {
		ApiResultVo result = new ApiResultVo(MessageEnum.ApiSucess.getMsg());
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
}
