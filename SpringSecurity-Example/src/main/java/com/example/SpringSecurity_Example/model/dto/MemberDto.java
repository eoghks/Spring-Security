package com.example.SpringSecurity_Example.model.dto;

import com.example.SpringSecurity_Example.model.entity.Member;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberDto {
	@NotBlank(message = "loginId를 입력하세요.")
	private String loginId;

	@NotBlank(message = "비밀번호를 입력하세요.")
	private String password;

	public Member toEntity() {
		return new Member(this.loginId, this.password);
	}
}
