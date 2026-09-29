package com.example.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 로그인 무차별 대입 방어 설정(계정별 잠금과 별개로 IP 단위로 제한한다).
 *
 * @param maxFailuresPerMinute 클라이언트 IP 당 1분 동안 허용하는 로그인 실패 횟수(도달 시 그 분 동안 429)
 */
@ConfigurationProperties(prefix = "app.security.login")
public record LoginProtectionProperties(int maxFailuresPerMinute) {
}
