package com.example.SpringSecurity_Example.model.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResultVo extends ApiResultVo{
	private String loginId;
	private String token;

	public LoginResultVo(String loginId, String token, String msg) {
		this.loginId = loginId;
		this.token =token;
		this.msg =msg;
	}
}
