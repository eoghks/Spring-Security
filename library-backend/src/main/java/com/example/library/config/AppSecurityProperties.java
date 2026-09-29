package com.example.library.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 애플리케이션 보안 설정.
 *
 * @param maxLoginAttempts 연속 로그인 실패 허용 횟수(도달 시 계정 잠금)
 * @param trustedProxies   X-Forwarded-For 를 신뢰할 프록시 IP·CIDR 목록
 */
@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(int maxLoginAttempts, List<String> trustedProxies) {

	public AppSecurityProperties {
		trustedProxies = trustedProxies == null ? List.of() : List.copyOf(trustedProxies);
	}
}
