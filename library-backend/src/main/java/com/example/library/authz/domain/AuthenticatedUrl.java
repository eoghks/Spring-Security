package com.example.library.authz.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인(사용자 인증)만 되어 있으면 호출 가능한 URL.
 */
@Entity
@Table(name = "authenticated_urls")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthenticatedUrl {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String httpMethod;

	private String urlPattern;

	private String description;
}
