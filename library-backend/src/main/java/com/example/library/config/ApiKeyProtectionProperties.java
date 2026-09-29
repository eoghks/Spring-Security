package com.example.library.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * API Key 무차별 대입·DB 부하 방어 설정.
 *
 * @param negativeCacheTtl     존재하지 않는 키 해시를 음성 캐시에 두는 시간(그동안 DB 를 다시 조회하지 않는다)
 * @param maxFailuresPerMinute 클라이언트 IP 당 1분 동안 허용하는 API Key 인증 실패 횟수(초과 시 429)
 */
@ConfigurationProperties(prefix = "app.security.api-key")
public record ApiKeyProtectionProperties(Duration negativeCacheTtl, int maxFailuresPerMinute) {
}
