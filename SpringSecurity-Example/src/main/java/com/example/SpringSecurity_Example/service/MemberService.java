package com.example.SpringSecurity_Example.service;

import com.example.SpringSecurity_Example.model.dto.MemberDto;

public interface MemberService {
	public void createUser(MemberDto memberDto) throws Exception;
	public String login(MemberDto memberDto) throws Exception;
}
