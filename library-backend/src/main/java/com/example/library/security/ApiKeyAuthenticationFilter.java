package com.example.library.security;

import com.example.library.apikey.application.ApiKeyUsageRecorder;
import com.example.library.apikey.domain.ApiKeyCodec;
import com.example.library.authz.cache.ApiKeySnapshot;
import com.example.library.authz.cache.AuthzCache;
import com.example.library.common.error.ErrorCode;
import com.example.library.common.error.ErrorResponseWriter;
import com.example.library.common.net.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * X-API-KEY 헤더 인증 필터(JWT 필터보다 앞).
 * 키가 제시됐는데 무효(형식 오류·미등록·폐기·만료)면 즉시 401 INVALID_API_KEY — JWT 로 폴백하지 않는다.
 * 인증 실패는 클라이언트 IP 별로 세고, 1분 허용치를 넘긴 IP 는 그 분이 끝날 때까지 조회 없이 429 로 거절한다.
 * 로그에는 키 원문 대신 앞 8자만 남긴다.
 */
@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-API-KEY";

	private final ApiKeyCodec apiKeyCodec;
	private final AuthzCache authzCache;
	private final ApiKeyUsageRecorder usageRecorder;
	private final AuthenticationEntryPoint authenticationEntryPoint;
	private final ApiKeyFailureLimiter failureLimiter;
	private final ClientIpResolver clientIpResolver;
	private final ErrorResponseWriter errorResponseWriter;
	private final Clock clock;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String rawKey = request.getHeader(HEADER);
		if (rawKey == null) {
			chain.doFilter(request, response);
			return;
		}
		String clientIp = clientIpResolver.resolve(request);
		if (failureLimiter.isBlocked(clientIp)) {
			response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(failureLimiter.retryAfter().toSeconds()));
			errorResponseWriter.write(request, response, ErrorCode.TOO_MANY_REQUESTS);
			return;
		}
		Optional<ApiKeySnapshot> apiKey = findUsableKey(rawKey.trim());
		if (apiKey.isEmpty()) {
			failureLimiter.recordFailure(clientIp);
			log.info("API Key 인증 실패: prefix={}", safePrefix(rawKey));
			authenticationEntryPoint.commence(request, response,
					new LibraryAuthenticationException(ErrorCode.INVALID_API_KEY));
			return;
		}
		authenticate(apiKey.get());
		usageRecorder.record(apiKey.get().apiKeyId());
		chain.doFilter(request, response);
	}

	private Optional<ApiKeySnapshot> findUsableKey(String rawKey) {
		if (!apiKeyCodec.hasValidFormat(rawKey)) {
			return Optional.empty();
		}
		LocalDateTime now = LocalDateTime.now(clock);
		return authzCache.findApiKey(apiKeyCodec.hash(rawKey)).filter(snapshot -> snapshot.isUsable(now));
	}

	private void authenticate(ApiKeySnapshot snapshot) {
		ApiKeyPrincipal principal = new ApiKeyPrincipal(snapshot.apiKeyId(), snapshot.name(),
				snapshot.actionCodes(), snapshot.allowedIps());
		SecurityContextHolder.getContext()
				.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
	}

	private String safePrefix(String rawKey) {
		return rawKey.length() >= ApiKeyCodec.DISPLAY_PREFIX_LENGTH
				? rawKey.substring(0, ApiKeyCodec.DISPLAY_PREFIX_LENGTH)
				: "(짧은 값)";
	}
}
