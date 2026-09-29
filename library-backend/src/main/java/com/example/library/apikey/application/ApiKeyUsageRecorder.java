package com.example.library.apikey.application;

import com.example.library.apikey.domain.ApiKeyRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * API Key 마지막 사용 시각 기록. 호출마다 UPDATE 하지 않도록 노드별로 1분에 한 번만 기록한다.
 */
@Component
@RequiredArgsConstructor
public class ApiKeyUsageRecorder {

	private static final Duration INTERVAL = Duration.ofMinutes(1);

	private final ApiKeyRepository apiKeyRepository;
	private final Clock clock;
	private final Map<Long, Instant> lastRecorded = new ConcurrentHashMap<>();

	public void record(Long apiKeyId) {
		Instant now = clock.instant();
		Instant previous = lastRecorded.get(apiKeyId);
		if (previous != null && Duration.between(previous, now).compareTo(INTERVAL) < 0) {
			return;
		}
		lastRecorded.put(apiKeyId, now);
		apiKeyRepository.touchLastUsed(apiKeyId, LocalDateTime.now(clock));
	}
}
