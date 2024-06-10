package com.example.SpringSecurity_Example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Users {
	@Id
	@Column(name ="user_id", columnDefinition ="int")
	private Long id;
	@Column(nullable = false)
	private String password;
	
	public Users(Long id, String password) {
		this.id =id;
		this.password = password;
	}
}
