package com.example.SpringSecurity_Example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.SpringSecurity_Example.model.Member;
import com.example.SpringSecurity_Example.model.MemberDto;
import com.example.SpringSecurity_Example.repository.MemberRepository;


@Service
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private BCryptPasswordEncoder encoder;

	@Override
	@Transactional
	public void createUser(MemberDto memberDto) throws Exception {
		Member checkMember = memberRepository.findByLoginId(memberDto.getLoginId());
		if(checkMember != null) {
			throw new Exception("중복된 로그인Id 입니다.");
		}
		memberDto.setPassword(encoder.encode(memberDto.getPassword()));
		Member member = memberDto.toEntity();
		memberRepository.save(member);
	}
}
