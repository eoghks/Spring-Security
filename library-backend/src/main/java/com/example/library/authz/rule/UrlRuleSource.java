package com.example.library.authz.rule;

import java.util.Optional;

/**
 * 규칙 테이블을 만들기 위한 원천 행(action_urls 또는 authenticated_urls 한 줄).
 *
 * @param httpMethod HTTP 메서드
 * @param urlPattern URL 패턴 문자열
 * @param actionCode 액션 코드(authenticated_urls 행이면 비어 있음)
 */
public record UrlRuleSource(String httpMethod, String urlPattern, Optional<String> actionCode) {

	public static UrlRuleSource forAction(String httpMethod, String urlPattern, String actionCode) {
		return new UrlRuleSource(httpMethod, urlPattern, Optional.of(actionCode));
	}

	public static UrlRuleSource forAuthenticated(String httpMethod, String urlPattern) {
		return new UrlRuleSource(httpMethod, urlPattern, Optional.empty());
	}
}
