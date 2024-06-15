package com.example.SpringSecurity_Example.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Member {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name ="memberid")
	private Long id;

	@Column(name ="loginid" ,nullable = false, unique = true)
	private String loginId;

	@Column(nullable = false)
	private String password;

	public Member() {

	}

	public Member(String loginId, String password) {
		this.loginId = loginId;
		this.password = password;
	}
}
