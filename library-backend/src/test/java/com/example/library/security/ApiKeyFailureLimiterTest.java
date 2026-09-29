package com.example.library.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.config.ApiKeyProtectionProperties;
import com.example.library.config.HazelcastConfig;
import com.example.library.config.HazelcastProperties;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * IP 별 API Key 인증 실패 제한(분 단위 버킷, 실제 Hazelcast 멤버).
 */
class ApiKeyFailureLimiterTest {

	private static final int LIMIT = 3;
	/** 분의 시작에서 10초 지난 시각 */
	private static final Instant START = Instant.parse("2026-09-29T01:00:10Z");

	private static HazelcastInstance hazelcast;

	@BeforeAll
	static void startMember() {
		HazelcastProperties properties = new HazelcastProperties("limiter-test-" + UUID.randomUUID(), 5981,
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
		ApiKeyFailureLimiter limiter = limiterAt(START);
		String ip = "198.51.100.1";

		for (int i = 0; i < LIMIT - 1; i++) {
			limiter.recordFailure(ip);
		}
		assertThat(limiter.isBlocked(ip)).isFalse();

		limiter.recordFailure(ip);
		assertThat(limiter.isBlocked(ip)).isTrue();
		assertThat(limiter.isBlocked("198.51.100.2")).isFalse();
	}

	@Test
	@DisplayName("다음 분이 되면 새 버킷이라 다시 허용된다")
	void resetsNextMinute() {
		String ip = "198.51.100.3";
		ApiKeyFailureLimiter limiter = limiterAt(START);
		for (int i = 0; i < LIMIT; i++) {
			limiter.recordFailure(ip);
		}
		assertThat(limiter.isBlocked(ip)).isTrue();

		assertThat(limiterAt(START.plusSeconds(49)).isBlocked(ip)).isTrue();
		assertThat(limiterAt(START.plusSeconds(50)).isBlocked(ip)).isFalse();
	}

	@Test
	@DisplayName("제한기 인스턴스(노드)가 달라도 같은 클러스터면 실패 횟수를 공유한다")
	void sharedAcrossLimiterInstances() {
		String ip = "198.51.100.4";
		for (int i = 0; i < LIMIT; i++) {
			limiterAt(START).recordFailure(ip);
		}

		assertThat(limiterAt(START).isBlocked(ip)).isTrue();
	}

	@Test
	@DisplayName("Retry-After 는 현재 분이 끝날 때까지 남은 초")
	void retryAfter() {
		assertThat(limiterAt(START).retryAfter()).isEqualTo(Duration.ofSeconds(50));
		assertThat(limiterAt(START.plusSeconds(49)).retryAfter()).isEqualTo(Duration.ofSeconds(1));
	}

	private ApiKeyFailureLimiter limiterAt(Instant now) {
		return new ApiKeyFailureLimiter(hazelcast, new ApiKeyProtectionProperties(Duration.ofSeconds(60), LIMIT),
				Clock.fixed(now, ZoneId.of("Asia/Seoul")));
	}
}
