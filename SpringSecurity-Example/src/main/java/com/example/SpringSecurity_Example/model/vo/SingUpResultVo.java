package com.example.SpringSecurity_Example.model.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SingUpResultVo extends ApiResultVo {
	private String loginId;

	public SingUpResultVo(String loginId, String msg) {
		this.loginId = loginId;
		this.msg =msg;
	}
}
