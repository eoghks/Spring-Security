package com.example.library.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LikePatternsTest {

	@Test
	@DisplayName("%, _, 이스케이프 문자 앞에 이스케이프 문자를 붙이고 나머지는 그대로 둔다")
	void escape() {
		assertThat(LikePatterns.escape("100%_완료")).isEqualTo("100\\%\\_완료");
		assertThat(LikePatterns.escape("a\\b")).isEqualTo("a\\\\b");
		assertThat(LikePatterns.escape("클린 코드")).isEqualTo("클린 코드");
		assertThat(LikePatterns.escape("")).isEmpty();
	}
}
