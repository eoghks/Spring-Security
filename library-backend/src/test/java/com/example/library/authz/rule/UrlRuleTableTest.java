package com.example.library.authz.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UrlRuleTableTest {

	private final UrlRuleTable table = UrlRuleTable.of(List.of(
			UrlRuleSource.forAction("GET", "/api/books", "BOOK:READ"),
			UrlRuleSource.forAction("GET", "/api/books", "BOOK_MANAGE:READ"),
			UrlRuleSource.forAction("GET", "/api/books/{id}", "BOOK:READ"),
			UrlRuleSource.forAction("GET", "/api/books/categories", "CATEGORY:READ"),
			UrlRuleSource.forAction("DELETE", "/api/books/{id}", "BOOK_MANAGE:DELETE"),
			UrlRuleSource.forAction("GET", "/api/admin/**", "ADMIN_ALL:READ"),
			UrlRuleSource.forAction("GET", "/api/admin/users", "USER_MANAGE:READ"),
			UrlRuleSource.forAuthenticated("GET", "/api/me")));

	@Test
	@DisplayName("같은 URL 에 여러 액션이 걸리면 한 규칙으로 합쳐지고, 그중 하나만 보유해도 통과한다(OR)")
	void orRule() {
		UrlRule rule = table.match("GET", "/api/books").orElseThrow();

		assertThat(rule.actionCodes()).containsExactlyInAnyOrder("BOOK:READ", "BOOK_MANAGE:READ");
		assertThat(rule.grantsAny(Set.of("BOOK_MANAGE:READ"))).isTrue();
		assertThat(rule.grantsAny(Set.of("BOOK:READ"))).isTrue();
		assertThat(rule.grantsAny(Set.of("MY_LOAN:READ"))).isFalse();
	}

	@Test
	@DisplayName("리터럴 패턴이 변수 패턴보다 우선한다")
	void literalBeatsVariable() {
		assertThat(table.match("GET", "/api/books/categories").orElseThrow().actionCodes())
				.containsExactly("CATEGORY:READ");
		assertThat(table.match("GET", "/api/books/7").orElseThrow().actionCodes())
				.containsExactly("BOOK:READ");
	}

	@Test
	@DisplayName("구체적인 패턴이 와일드카드 패턴보다 우선한다")
	void specificBeatsWildcard() {
		assertThat(table.match("GET", "/api/admin/users").orElseThrow().actionCodes())
				.containsExactly("USER_MANAGE:READ");
		assertThat(table.match("GET", "/api/admin/other").orElseThrow().actionCodes())
				.containsExactly("ADMIN_ALL:READ");
	}

	@Test
	@DisplayName("미등록 URL·메서드 불일치는 규칙이 없다(호출자가 거부 — fail-closed)")
	void unregistered() {
		assertThat(table.match("GET", "/api/unknown")).isEmpty();
		assertThat(table.match("POST", "/api/books")).isEmpty();
		assertThat(table.match("GET", "/api/books/")).isEmpty();
	}

	@Test
	@DisplayName("authenticated_urls 규칙은 액션 없이 로그인만 요구한다")
	void authenticatedOnly() {
		UrlRule rule = table.match("get", "/api/me").orElseThrow();

		assertThat(rule.authenticatedOnly()).isTrue();
		assertThat(rule.actionCodes()).isEmpty();
	}

	@Test
	@DisplayName("보유 액션으로 호출 가능한 URL 은 등록 패턴 문자열 그대로 돌려준다")
	void callableUrls() {
		assertThat(table.callableUrls(Set.of("BOOK_MANAGE:READ", "BOOK_MANAGE:DELETE")))
				.containsExactly("DELETE /api/books/{id}", "GET /api/books");
	}
}
