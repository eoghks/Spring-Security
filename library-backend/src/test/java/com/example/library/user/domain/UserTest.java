package com.example.library.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

	@Test
	@DisplayName("로그인 5회 실패 시 계정이 잠기고, 잠금 해제 시 실패 횟수가 초기화된다")
	void lockAfterMaxFailures() {
		User user = User.builder().username("u").password("p").name("n").email("e").build();
		LocalDateTime now = LocalDateTime.now();

		for (int i = 0; i < 4; i++) {
			assertThat(user.recordLoginFailure(5, now)).isFalse();
		}
		assertThat(user.recordLoginFailure(5, now)).isTrue();
		assertThat(user.isLocked()).isTrue();

		user.unlock();
		assertThat(user.isLocked()).isFalse();
		assertThat(user.getFailedLoginCount()).isZero();
	}
}
