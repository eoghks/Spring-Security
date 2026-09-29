package com.example.library.authz.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ActionUrlRepositoryTest {

	@Autowired
	private ActionUrlRepository actionUrlRepository;

	@Test
	@DisplayName("같은 URL 을 여러 액션에 매핑할 수 있다(도서 조회 URL 이 BOOK·BOOK_MANAGE 에 모두 존재)")
	void sameUrlMappedToMultipleActions() {
		List<String> codes = actionUrlRepository.findAllWithAction().stream()
				.filter(url -> url.getHttpMethod().equals("GET") && url.getUrlPattern().equals("/api/books"))
				.map(url -> url.getAction().authorityCode())
				.toList();

		assertThat(codes).containsExactlyInAnyOrder("BOOK:READ", "BOOK_MANAGE:READ");
	}
}
