package com.example.library.authz.domain;

/**
 * 액션 유형.
 */
public enum ActionType {
	/** 화면 진입·조회. 메뉴 표시 여부는 이 유형의 보유로 판정한다 */
	READ,
	/** 화면 안의 버튼 동작 */
	ACTION
}
