package com.example.library.common.api;

/**
 * LIKE 검색어 이스케이프. 검색어에 든 %·_ 가 와일드카드로 해석되지 않게 한다(파라미터 바인딩과 별개인 기능 문제).
 * 리포지토리 쿼리는 {@code like ... escape '\'} 로 같은 이스케이프 문자를 선언해야 한다.
 */
public final class LikePatterns {

	/** 쿼리의 escape 절과 맞춘 이스케이프 문자 */
	public static final char ESCAPE = '\\';

	private LikePatterns() {
	}

	/** 이스케이프 문자 자신과 %, _ 앞에 이스케이프 문자를 붙인다 */
	public static String escape(String keyword) {
		StringBuilder escaped = new StringBuilder(keyword.length());
		for (char ch : keyword.toCharArray()) {
			if (ch == ESCAPE || ch == '%' || ch == '_') {
				escaped.append(ESCAPE);
			}
			escaped.append(ch);
		}
		return escaped.toString();
	}
}
