package com.example.library.security;

import com.example.library.authz.cache.CacheNames;
import com.example.library.common.net.IpPatterns;
import com.example.library.config.ApiKeyProtectionProperties;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.IMap;
import java.io.Serial;
import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 클라이언트 IP 별 API Key 인증 실패 횟수 제한(고정 1분 창).
 * 실패 횟수는 Hazelcast IMap 의 "IP|분" 버킷에 원자적으로 더해 모든 노드가 같은 값을 본다.
 * 한 분 동안 허용 횟수를 다 쓰면 그 분이 끝날 때까지 해당 IP 의 API Key 요청은 조회 없이 거절된다.
 */
@Slf4j
@Component
public class ApiKeyFailureLimiter {

	private static final long MINUTE_SECONDS = 60;

	private final IMap<String, Integer> failures;
	private final int maxFailuresPerMinute;
	private final Clock clock;

	public ApiKeyFailureLimiter(HazelcastInstance hazelcast, ApiKeyProtectionProperties properties, Clock clock) {
		this.failures = hazelcast.getMap(CacheNames.API_KEY_FAILURES);
		this.maxFailuresPerMinute = properties.maxFailuresPerMinute();
		this.clock = clock;
	}

	/** 이번 분의 실패 횟수가 이미 허용치에 도달했으면 true */
	public boolean isBlocked(String clientIp) {
		return Optional.ofNullable(failures.get(bucketKey(clientIp)))
				.map(count -> count >= maxFailuresPerMinute)
				.orElse(false);
	}

	/** 실패 1회를 기록한다. 허용치에 막 도달한 순간 한 번만 경고 로그를 남긴다 */
	public void recordFailure(String clientIp) {
		int count = failures.executeOnKey(bucketKey(clientIp), new IncrementProcessor());
		if (count == maxFailuresPerMinute) {
			log.warn("API Key 인증 실패 한도 도달 — 이번 분 동안 차단: ip={}, 한도={}회/분", IpPatterns.mask(clientIp),
					maxFailuresPerMinute);
		}
	}

	/** 현재 분이 끝날 때까지 남은 시간(Retry-After 용, 최소 1초) */
	public Duration retryAfter() {
		long elapsed = clock.instant().getEpochSecond() % MINUTE_SECONDS;
		return Duration.ofSeconds(MINUTE_SECONDS - elapsed);
	}

	private String bucketKey(String clientIp) {
		return clientIp + "|" + clock.instant().getEpochSecond() / MINUTE_SECONDS;
	}

	/** 버킷 값을 1 늘리고 새 값을 돌려준다(키 소유 파티션에서 원자적으로 실행) */
	static final class IncrementProcessor implements EntryProcessor<String, Integer, Integer>, Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

		@Override
		public Integer process(Map.Entry<String, Integer> entry) {
			int next = Optional.ofNullable(entry.getValue()).orElse(0) + 1;
			entry.setValue(next);
			return next;
		}
	}
}
