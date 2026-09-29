package com.example.library.security;

import com.example.library.auth.token.AccessTokenResult;
import com.example.library.auth.token.JwtProvider;
import com.example.library.authz.cache.AuthzCache;
import com.example.library.authz.cache.UserAuthSnapshot;
import com.example.library.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer Access JWT 인증 필터.
 * 토큰이 없거나 무효면 인증하지 않고 사유만 요청 속성에 남긴다 — permitAll URL 은 그대로 통과하고,
 * 보호 URL 은 인가 단계에서 거부되어 EntryPoint 가 사유 코드로 401 을 응답한다.
 * 역할·잠금 여부는 토큰이 아니라 캐시에서 읽는다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtProvider jwtProvider;
	private final AuthzCache authzCache;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		boolean alreadyAuthenticated = SecurityContextHolder.getContext().getAuthentication() != null;
		Optional<String> token = bearerToken(request);
		if (!alreadyAuthenticated && token.isPresent()) {
			authenticate(token.get(), request);
		}
		chain.doFilter(request, response);
	}

	private void authenticate(String token, HttpServletRequest request) {
		switch (jwtProvider.verify(token)) {
			case AccessTokenResult.Valid valid -> authenticateUser(valid.userId(), request);
			case AccessTokenResult.Expired expired -> markFailure(request, ErrorCode.TOKEN_EXPIRED);
			case AccessTokenResult.Invalid invalid -> markFailure(request, ErrorCode.INVALID_TOKEN);
		}
	}

	private void authenticateUser(Long userId, HttpServletRequest request) {
		Optional<UserAuthSnapshot> snapshot = authzCache.findUser(userId);
		if (snapshot.isEmpty()) {
			markFailure(request, ErrorCode.INVALID_TOKEN);
			return;
		}
		if (snapshot.get().locked()) {
			markFailure(request, ErrorCode.ACCOUNT_LOCKED);
			return;
		}
		UserAuthSnapshot user = snapshot.get();
		UserPrincipal principal = new UserPrincipal(user.userId(), user.username(), user.roleId());
		SecurityContextHolder.getContext()
				.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
	}

	private void markFailure(HttpServletRequest request, ErrorCode errorCode) {
		request.setAttribute(JsonAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE, errorCode);
	}

	private Optional<String> bearerToken(HttpServletRequest request) {
		return Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
				.filter(header -> header.startsWith(BEARER_PREFIX))
				.map(header -> header.substring(BEARER_PREFIX.length()).trim())
				.filter(value -> !value.isEmpty());
	}
}
