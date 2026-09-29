package com.example.library.authz.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메뉴 안의 액션. 전역 식별 코드는 "메뉴코드:액션코드" 이다(예: BOOK_MANAGE:CREATE).
 */
@Entity
@Table(name = "menu_actions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MenuAction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "menu_id")
	private Menu menu;

	private String code;

	private String name;

	@Enumerated(EnumType.STRING)
	private ActionType actionType;

	/** 메뉴코드:액션코드 형식의 전역 액션 코드 */
	public String authorityCode() {
		return menu.getCode() + ":" + code;
	}

	public boolean isRead() {
		return actionType == ActionType.READ;
	}
}
