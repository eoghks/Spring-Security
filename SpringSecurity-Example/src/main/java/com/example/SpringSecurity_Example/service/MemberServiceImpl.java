package com.example.SpringSecurity_Example.service;

import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.SpringSecurity_Example.config.jwt.JwtUtil;
import com.example.SpringSecurity_Example.model.constant.RoleType;
import com.example.SpringSecurity_Example.model.dto.MemberDto;
import com.example.SpringSecurity_Example.model.entity.Member;
import com.example.SpringSecurity_Example.model.error.UserDefinedException;
import com.example.SpringSecurity_Example.model.security.CustomMemberInfoDto;
import com.example.SpringSecurity_Example.repository.MemberRepository;

import lombok.RequiredArgsConstructor;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
	private final MemberRepository memberRepository;
	private final BCryptPasswordEncoder encoder;
	private final ModelMapper modelMapper;
	private final JwtUtil jwtUtil;

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

	@Override
	public String login(MemberDto memberDto) throws Exception {
		Member member = memberRepository.findByLoginId(memberDto.getLoginId());
		if(member == null || encoder.matches(memberDto.getPassword(), member.getPassword()) == false){
			throw new UserDefinedException("id 또는 password를 확인하세요.");
		}
		CustomMemberInfoDto info = modelMapper.map(member, CustomMemberInfoDto.class);
		if(member.getId() == 1) {
			info.setRole(RoleType.Admin);
		}
		return jwtUtil.generatedJwtToken(info);
	}
}
