package com.example.library.authz.api;

import java.util.List;

/**
 * 메뉴 → 액션 → URL 트리(역할·권한 관리 화면용).
 */
public record MenuTreeResponse(Long id, String code, String name, String path, int sortOrder,
		List<ActionNode> actions) {

	/**
	 * 액션.
	 *
	 * @param authorityCode "메뉴코드:액션코드"
	 * @param actionType    READ / ACTION
	 */
	public record ActionNode(Long id, String code, String name, String actionType, String authorityCode,
			List<UrlNode> urls) {
	}

	/** 액션 URL */
	public record UrlNode(Long id, String httpMethod, String urlPattern) {
	}
}
