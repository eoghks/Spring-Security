package com.example.library.common.net;

import com.example.library.config.AppSecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 클라이언트 IP 결정.
 * 기본은 request.getRemoteAddr() 이고, 직전 홉이 신뢰 프록시(app.security.trusted-proxies)일 때만 X-Forwarded-For 를 본다.
 * XFF 는 오른쪽(가까운 홉)부터 거슬러 올라가며 첫 번째 비신뢰 주소를 클라이언트로 본다 — 클라이언트가 왼쪽에 임의 값을 넣어도 속지 않는다.
 * 모든 홉이 신뢰 대역이면(클라이언트가 신뢰 대역 안에 있는 경우) 맨 왼쪽 값은 클라이언트가 위조할 수 있으므로,
 * 우리 프록시가 직접 적은 맨 오른쪽 홉을 쓴다. 신뢰 목록에는 프록시 자신의 주소(/32)만 넣는 것을 권장한다.
 */
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

	private static final String X_FORWARDED_FOR = "X-Forwarded-For";

	private final AppSecurityProperties properties;

	public String resolve(HttpServletRequest request) {
		String remoteAddr = request.getRemoteAddr();
		String forwardedFor = request.getHeader(X_FORWARDED_FOR);
		if (forwardedFor == null || forwardedFor.isBlank() || !isTrustedProxy(remoteAddr)) {
			return remoteAddr;
		}
		List<String> hops = List.of(forwardedFor.split(",")).stream().map(String::trim).toList();
		for (int i = hops.size() - 1; i >= 0; i--) {
			String hop = hops.get(i);
			if (!IpPatterns.isValid(hop)) {
				return remoteAddr;
			}
			if (!isTrustedProxy(hop)) {
				return hop;
			}
		}
		// 전부 신뢰 대역이면 위조 가능한 왼쪽 값 대신 가장 가까운 프록시가 적은 값을 쓴다
		return hops.getLast();
	}

	private boolean isTrustedProxy(String address) {
		List<String> trusted = properties.trustedProxies();
		return !trusted.isEmpty() && IpPatterns.matchesAny(trusted, address);
	}
}
