package com.example.library.security;

import com.example.library.authz.cache.CacheNames;
import com.example.library.common.net.IpPatterns;
import com.example.library.config.LoginProtectionProperties;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import java.time.Clock;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 클라이언트 IP 별 로그인 실패 횟수 제한(고정 1분 창).
 * 계정별 5회 잠금은 한 계정만 보호하므로, 여러 계정에 비밀번호를 뿌리는 시도(password spraying)는 IP 단위로 막는다.
 * 카운터는 API Key 실패 제한과 같은 방식(Hazelcast IMap "IP|분" 버킷, 원자 증가)으로 모든 노드가 공유한다.
 */
@Slf4j
@Component
public class LoginFailureLimiter {

	private static final long MINUTE_SECONDS = 60;

	private final IMap<String, Integer> failures;
	private final int maxFailuresPerMinute;
	private final Clock clock;

	public LoginFailureLimiter(HazelcastInstance hazelcast, LoginProtectionProperties properties, Clock clock) {
		this.failures = hazelcast.getMap(CacheNames.LOGIN_FAILURES);
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
		int count = failures.executeOnKey(bucketKey(clientIp), new ApiKeyFailureLimiter.IncrementProcessor());
		if (count == maxFailuresPerMinute) {
			log.warn("로그인 실패 한도 도달 — 이번 분 동안 차단: ip={}, 한도={}회/분", IpPatterns.mask(clientIp),
					maxFailuresPerMinute);
		}
	}

	private String bucketKey(String clientIp) {
		return clientIp + "|" + clock.instant().getEpochSecond() / MINUTE_SECONDS;
	}
}
