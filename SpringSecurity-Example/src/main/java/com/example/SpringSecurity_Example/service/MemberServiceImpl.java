package com.example.SpringSecurity_Example.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.SpringSecurity_Example.model.dto.MemberDto;
import com.example.SpringSecurity_Example.model.entity.Member;
import com.example.SpringSecurity_Example.model.error.UserDefinedException;
import com.example.SpringSecurity_Example.repository.MemberRepository;

import lombok.RequiredArgsConstructor;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
	private final MemberRepository memberRepository;
	private final BCryptPasswordEncoder encoder;

	@Override
	@Transactional
	public void createUser(MemberDto memberDto) throws Exception {
		Member checkMember = memberRepository.findByLoginId(memberDto.getLoginId());
		if(checkMember != null) {
			throw new UserDefinedException("중복된 로그인Id 입니다.");
		}
		Member member = memberDto.toEntity();
		member.setPassword(encoder.encode(member.getPassword()));
		memberRepository.save(member);
	}
}
