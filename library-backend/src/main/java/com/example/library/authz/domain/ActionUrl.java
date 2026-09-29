package com.example.library.authz.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 액션이 허용하는 HTTP 메서드 + URL 패턴.
 */
@Entity
@Table(name = "action_urls")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActionUrl {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "action_id")
	private MenuAction action;

	private String httpMethod;

	private String urlPattern;

	@Builder
	private ActionUrl(MenuAction action, String httpMethod, String urlPattern) {
		this.action = action;
		this.httpMethod = httpMethod;
		this.urlPattern = urlPattern;
	}
}
