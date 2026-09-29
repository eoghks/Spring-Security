package com.example.library.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.config.HazelcastConfig;
import com.example.library.config.HazelcastProperties;
import com.example.library.config.LoginProtectionProperties;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * IP 별 로그인 실패 제한(분 단위 버킷, 실제 Hazelcast 멤버).
 */
class LoginFailureLimiterTest {

	private static final int LIMIT = 3;
	/** 분의 시작에서 10초 지난 시각 */
	private static final Instant START = Instant.parse("2026-09-29T01:00:10Z");

	private static HazelcastInstance hazelcast;

	@BeforeAll
	static void startMember() {
		HazelcastProperties properties = new HazelcastProperties("login-limiter-test-" + UUID.randomUUID(), 5961,
				List.of("127.0.0.1"), 600, "127.0.0.1");
		hazelcast = Hazelcast.newHazelcastInstance(HazelcastConfig.createConfig(properties));
	}

	@AfterAll
	static void stopMember() {
		hazelcast.shutdown();
	}

	@Test
	@DisplayName("한도만큼 실패하면 그 IP 만 차단되고 다른 IP 는 영향이 없다")
	void blocksAfterLimit() {
		LoginFailureLimiter limiter = limiterAt(START);
		String ip = "198.51.100.11";

		for (int i = 0; i < LIMIT - 1; i++) {
			limiter.recordFailure(ip);
		}
		assertThat(limiter.isBlocked(ip)).isFalse();

		limiter.recordFailure(ip);
		assertThat(limiter.isBlocked(ip)).isTrue();
		assertThat(limiter.isBlocked("198.51.100.12")).isFalse();
	}

	@Test
	@DisplayName("다음 분이 되면 새 버킷이라 다시 허용된다")
	void resetsNextMinute() {
		String ip = "198.51.100.13";
		for (int i = 0; i < LIMIT; i++) {
			limiterAt(START).recordFailure(ip);
		}

		assertThat(limiterAt(START.plusSeconds(49)).isBlocked(ip)).isTrue();
		assertThat(limiterAt(START.plusSeconds(50)).isBlocked(ip)).isFalse();
	}

	private LoginFailureLimiter limiterAt(Instant now) {
		return new LoginFailureLimiter(hazelcast, new LoginProtectionProperties(LIMIT),
				Clock.fixed(now, ZoneId.of("Asia/Seoul")));
	}
}
