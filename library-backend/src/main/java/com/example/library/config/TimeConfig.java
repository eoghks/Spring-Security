package com.example.library.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 시각 의존 로직(토큰 만료, 대출 기한, 접속 조건)이 테스트에서 시각을 고정할 수 있도록 Clock 을 빈으로 둔다.
 */
@Configuration
public class TimeConfig {

	@Bean
	public Clock clock(@Value("${app.zone-id:Asia/Seoul}") String zoneId) {
		return Clock.system(ZoneId.of(zoneId));
	}
}
