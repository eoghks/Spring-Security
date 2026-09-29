package com.example.library.authz.cache;

import com.example.library.config.ApiKeyProtectionProperties;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 권한 판정용 분산 캐시(Hazelcast IMap).
 * 캐시 미스면 반드시 DB 에서 읽어 채운다 — 미스를 "통과"로 취급하지 않는다.
 * 관리자가 권한을 바꾸면 해당 키만 evict 해 모든 노드에서 다음 요청부터 새 값이 적용된다.
 * 존재하지 않는 API Key 해시는 짧은 TTL 로 음성 캐시해 같은 무효 키의 반복 DB 조회를 막는다.
 */
@Slf4j
@Component
public class AuthzCache {

	private final IMap<Long, UserAuthSnapshot> users;
	private final IMap<Long, RoleActionsSnapshot> roles;
	private final IMap<String, ApiKeySnapshot> apiKeys;
	private final IMap<String, Boolean> apiKeyMisses;
	private final AuthzSnapshotLoader loader;
	private final long negativeCacheTtlMillis;

	public AuthzCache(HazelcastInstance hazelcast, AuthzSnapshotLoader loader,
			ApiKeyProtectionProperties protectionProperties) {
		this.users = hazelcast.getMap(CacheNames.USER_AUTH);
		this.roles = hazelcast.getMap(CacheNames.ROLE_ACTIONS);
		this.apiKeys = hazelcast.getMap(CacheNames.API_KEYS);
		this.apiKeyMisses = hazelcast.getMap(CacheNames.API_KEY_MISSES);
		this.loader = loader;
		this.negativeCacheTtlMillis = protectionProperties.negativeCacheTtl().toMillis();
	}

	/** 사용자 스냅샷 조회(없으면 DB 적재). 존재하지 않는 사용자는 캐시하지 않는다 */
	public Optional<UserAuthSnapshot> findUser(Long userId) {
		Optional<UserAuthSnapshot> cached = Optional.ofNullable(users.get(userId));
		if (cached.isPresent()) {
			return cached;
		}
		Optional<UserAuthSnapshot> loaded = loader.loadUser(userId);
		loaded.ifPresent(snapshot -> users.set(userId, snapshot));
		return loaded;
	}

	/** 역할 보유 액션 코드 조회(없으면 DB 적재) */
	public Set<String> roleActionCodes(Long roleId) {
		return Optional.ofNullable(roles.get(roleId))
				.orElseGet(() -> loadRole(roleId))
				.actionCodes();
	}

	private RoleActionsSnapshot loadRole(Long roleId) {
		RoleActionsSnapshot loaded = loader.loadRoleActions(roleId);
		roles.set(roleId, loaded);
		return loaded;
	}

	/**
	 * API Key 스냅샷 조회(없으면 DB 적재).
	 * DB 에도 없는 해시는 음성 캐시에 TTL 동안 두고, 그동안은 DB 를 다시 조회하지 않고 빈 값을 돌려준다.
	 */
	public Optional<ApiKeySnapshot> findApiKey(String keyHash) {
		Optional<ApiKeySnapshot> cached = Optional.ofNullable(apiKeys.get(keyHash));
		if (cached.isPresent() || apiKeyMisses.containsKey(keyHash)) {
			return cached;
		}
		Optional<ApiKeySnapshot> loaded = loader.loadApiKey(keyHash);
		loaded.ifPresentOrElse(snapshot -> apiKeys.set(keyHash, snapshot),
				() -> apiKeyMisses.set(keyHash, Boolean.TRUE, negativeCacheTtlMillis, TimeUnit.MILLISECONDS));
		return loaded;
	}

	public void evictUser(Long userId) {
		users.delete(userId);
		log.info("사용자 권한 캐시 evict: userId={}", userId);
	}

	public void evictRole(Long roleId) {
		roles.delete(roleId);
		log.info("역할 권한 캐시 evict: roleId={}", roleId);
	}

	/** 스냅샷과 음성 캐시를 함께 지운다(발급 직후 같은 해시가 "없음"으로 남지 않게) */
	public void evictApiKey(String keyHash) {
		apiKeys.delete(keyHash);
		apiKeyMisses.delete(keyHash);
		log.info("API Key 캐시 evict");
	}
}
