package com.example.SpringSecurity_Example.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UsersDto {
	@NotNull(message = "아이디를 입력하세요")
	private Long id;
	@NotBlank(message = "비밀번호를 입력하세요")
	private String password;
	
	public Users toEntity() {
		return new Users(this.id, this.password);
	}
}
