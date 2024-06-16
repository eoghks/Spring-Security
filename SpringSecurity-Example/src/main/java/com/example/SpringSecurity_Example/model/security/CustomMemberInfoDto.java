package com.example.SpringSecurity_Example.model.security;

import com.example.SpringSecurity_Example.model.constant.RoleType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomMemberInfoDto {
	private Long memberId;
	private String loginId;
	private String password;
	private RoleType role = RoleType.User;
}
