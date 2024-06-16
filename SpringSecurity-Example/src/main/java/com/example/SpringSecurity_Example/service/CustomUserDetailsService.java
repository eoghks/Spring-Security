package com.example.SpringSecurity_Example.service;

import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.SpringSecurity_Example.model.constant.RoleType;
import com.example.SpringSecurity_Example.model.entity.Member;
import com.example.SpringSecurity_Example.model.security.CustomMemberInfoDto;
import com.example.SpringSecurity_Example.model.security.CustomUserDetails;
import com.example.SpringSecurity_Example.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService{
	private final ModelMapper modelMapper;
	private final MemberRepository memberRepository;

	// JwtAuthFilter에서 JWT 유효성 검증 후 JWT에서 추출한 유저 식별자와 일치하는 유저가 있는지 확인, 있다면 UserDeatils 객체 반환
	@Override
	public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
		Member member = memberRepository.findById(Long.parseLong(id))
				.orElseThrow(() -> new UsernameNotFoundException("User 정보를 찾을 수 없습니다."));
		CustomMemberInfoDto info = modelMapper.map(member, CustomMemberInfoDto.class);
		if(member.getId() == 1) {
			info.setRole(RoleType.Admin);
		}
		return new CustomUserDetails(info);
	}

}
