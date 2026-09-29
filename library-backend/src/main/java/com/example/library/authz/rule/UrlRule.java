package com.example.library.authz.rule;

import java.util.Set;
import org.springframework.web.util.pattern.PathPattern;

/**
 * (HTTP 메서드, URL 패턴) 하나에 대한 인가 규칙.
 * 같은 URL 이 여러 액션에 등록되면 액션 코드를 합쳐 한 규칙으로 만든다 — 그중 하나만 보유해도 통과(OR).
 *
 * @param httpMethod        HTTP 메서드(대문자)
 * @param pathPattern       파싱된 URL 패턴
 * @param actionCodes       통과시키는 액션 코드 집합("메뉴코드:액션코드")
 * @param authenticatedOnly 로그인한 사용자면 액션과 무관하게 통과(authenticated_urls)
 */
public record UrlRule(String httpMethod, PathPattern pathPattern, Set<String> actionCodes,
		boolean authenticatedOnly) {

	public UrlRule {
		actionCodes = Set.copyOf(actionCodes);
	}

	/** 등록된 패턴 문자열 그대로의 "METHOD /pattern" 키 — 프론트 &lt;Can url&gt; 비교 기준 */
	public String key() {
		return httpMethod + " " + pathPattern.getPatternString();
	}

	/** 보유 액션 중 하나라도 이 규칙의 액션이면 true (OR) */
	public boolean grantsAny(Set<String> grantedActionCodes) {
		return actionCodes.stream().anyMatch(grantedActionCodes::contains);
	}
}
