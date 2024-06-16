package com.example.SpringSecurity_Example.model.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails{
	private final CustomMemberInfoDto info;

	public CustomUserDetails(CustomMemberInfoDto info) {
		this.info=info;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		List<String> roles = new ArrayList<>();
		roles.add("ROLE_" + info.getRole().getRole());

		return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
	}

	@Override
	public String getPassword() {
		return info.getPassword();
	}

	@Override
	public String getUsername() {
		return info.getLoginId();
	}

}
