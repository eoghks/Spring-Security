package com.example.SpringSecurity_Example.model.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiResultVo {
	String msg;

	public ApiResultVo() {

	}

	public ApiResultVo(String msg) {
		this.msg =msg;
	}
}
