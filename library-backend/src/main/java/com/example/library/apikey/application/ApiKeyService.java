package com.example.library.apikey.application;

import com.example.library.apikey.api.ApiKeyIssueRequest;
import com.example.library.apikey.api.ApiKeyResponse;
import com.example.library.apikey.api.IssuedApiKeyResponse;
import com.example.library.apikey.domain.ApiKey;
import com.example.library.apikey.domain.ApiKeyCodec;
import com.example.library.apikey.domain.ApiKeyCodec.IssuedKey;
import com.example.library.apikey.domain.ApiKeyRepository;
import com.example.library.authz.cache.AuthzChangedEvent;
import com.example.library.authz.cache.AuthzSnapshotLoader;
import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import com.example.library.security.GrantedActionResolver;
import com.example.library.security.UserPrincipal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API Key 발급·조회·폐기.
 * 권한 상승을 막기 위해 발급자는 자신이 보유한 액션만 키에 부여할 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApiKeyService {

	private final ApiKeyRepository apiKeyRepository;
	private final ApiKeyCodec apiKeyCodec;
	private final AuthzSnapshotLoader snapshotLoader;
	private final GrantedActionResolver grantedActionResolver;
	private final ApplicationEventPublisher eventPublisher;
	private final Clock clock;

	@Transactional(readOnly = true)
	public List<ApiKeyResponse> list() {
		LocalDateTime now = LocalDateTime.now(clock);
		return apiKeyRepository.findAllByOrderByIdDesc().stream()
				.map(apiKey -> ApiKeyResponse.of(apiKey, snapshotLoader.toActionCodes(apiKey.getActionIds()), now))
				.toList();
	}

	public IssuedApiKeyResponse issue(UserPrincipal issuer, ApiKeyIssueRequest request) {
		Set<Long> actionIds = new HashSet<>(request.actionIds());
		Set<String> actionCodes = snapshotLoader.toActionCodes(actionIds);
		if (actionCodes.size() != actionIds.size()) {
			throw new BusinessException(ErrorCode.INVALID_ACTION);
		}
		if (!grantedActionResolver.grantedActions(issuer).containsAll(actionCodes)) {
			throw new BusinessException(ErrorCode.ACCESS_DENIED);
		}
		IssuedKey key = apiKeyCodec.generate();
		LocalDateTime now = LocalDateTime.now(clock);
		ApiKey saved = apiKeyRepository.save(ApiKey.builder()
				.name(request.name())
				.keyPrefix(key.prefix())
				.keyHash(key.hash())
				.ownerUserId(issuer.userId())
				.expiresAt(request.expiresAt())
				.createdAt(now)
				.actionIds(actionIds)
				// 검증(@IpOrCidr)은 앞뒤 공백을 허용하므로 저장 전에 접속 조건과 같은 방식으로 다듬는다
				.allowedIps(request.allowedIps().stream().map(String::trim).collect(Collectors.toSet()))
				.build());
		// 같은 해시가 음성 캐시("없는 키")에 남아 있지 않도록 커밋 후 지운다
		eventPublisher.publishEvent(new AuthzChangedEvent.ApiKeyChanged(key.hash()));
		log.info("API Key 발급: id={}, prefix={}, issuer={}", saved.getId(), key.prefix(), issuer.userId());
		return new IssuedApiKeyResponse(key.raw(), ApiKeyResponse.of(saved, actionCodes, now));
	}

	public ApiKeyResponse revoke(Long id) {
		ApiKey apiKey = apiKeyRepository.findById(id)
				.orElseThrow(() -> new BusinessException(ErrorCode.API_KEY_NOT_FOUND));
		if (apiKey.isRevoked()) {
			throw new BusinessException(ErrorCode.API_KEY_ALREADY_REVOKED);
		}
		LocalDateTime now = LocalDateTime.now(clock);
		apiKey.revoke(now);
		eventPublisher.publishEvent(new AuthzChangedEvent.ApiKeyChanged(apiKey.getKeyHash()));
		log.info("API Key 폐기: id={}, prefix={}", id, apiKey.getKeyPrefix());
		return ApiKeyResponse.of(apiKey, snapshotLoader.toActionCodes(apiKey.getActionIds()), now);
	}
}
