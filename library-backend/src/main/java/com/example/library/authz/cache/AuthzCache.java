package com.example.library.authz.cache;

import com.example.library.config.ApiKeyProtectionProperties;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.IMap;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
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
 *
 * <p>적재·evict 경합 방지: evict 는 키별 세대 번호를 먼저 올린 뒤 값을 지운다. 적재는 DB 를 읽기 전 세대를 기억해 두고,
 * 값을 넣은 뒤 세대가 바뀌었으면(그 사이 evict 가 끼어들었으면) 방금 넣은 값을 지운다.
 * 그래서 "미스 → 옛 DB 값 읽기 → (변경 커밋·evict) → 옛 값 저장" 순서가 되어도 옛 권한이 TTL 동안 남지 않는다.
 */
@Slf4j
@Component
public class AuthzCache {

	private final IMap<Long, UserAuthSnapshot> users;
	private final IMap<Long, RoleActionsSnapshot> roles;
	private final IMap<String, ApiKeySnapshot> apiKeys;
	private final IMap<String, Boolean> apiKeyMisses;
	private final IMap<String, Long> generations;
	private final AuthzSnapshotLoader loader;
	private final long negativeCacheTtlMillis;

	public AuthzCache(HazelcastInstance hazelcast, AuthzSnapshotLoader loader,
			ApiKeyProtectionProperties protectionProperties) {
		this.users = hazelcast.getMap(CacheNames.USER_AUTH);
		this.roles = hazelcast.getMap(CacheNames.ROLE_ACTIONS);
		this.apiKeys = hazelcast.getMap(CacheNames.API_KEYS);
		this.apiKeyMisses = hazelcast.getMap(CacheNames.API_KEY_MISSES);
		this.generations = hazelcast.getMap(CacheNames.AUTHZ_GENERATIONS);
		this.loader = loader;
		this.negativeCacheTtlMillis = protectionProperties.negativeCacheTtl().toMillis();
	}

	/** 사용자 스냅샷 조회(없으면 DB 적재). 존재하지 않는 사용자는 캐시하지 않는다 */
	public Optional<UserAuthSnapshot> findUser(Long userId) {
		Optional<UserAuthSnapshot> cached = Optional.ofNullable(users.get(userId));
		if (cached.isPresent()) {
			return cached;
		}
		String generationKey = userGeneration(userId);
		long before = generation(generationKey);
		Optional<UserAuthSnapshot> loaded = loader.loadUser(userId);
		loaded.ifPresent(snapshot -> {
			users.set(userId, snapshot);
			discardIfEvicted(generationKey, before, () -> users.delete(userId));
		});
		return loaded;
	}

	/** 역할 보유 액션 코드 조회(없으면 DB 적재) */
	public Set<String> roleActionCodes(Long roleId) {
		return Optional.ofNullable(roles.get(roleId))
				.orElseGet(() -> loadRole(roleId))
				.actionCodes();
	}

	private RoleActionsSnapshot loadRole(Long roleId) {
		String generationKey = roleGeneration(roleId);
		long before = generation(generationKey);
		RoleActionsSnapshot loaded = loader.loadRoleActions(roleId);
		roles.set(roleId, loaded);
		discardIfEvicted(generationKey, before, () -> roles.delete(roleId));
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
		String generationKey = apiKeyGeneration(keyHash);
		long before = generation(generationKey);
		Optional<ApiKeySnapshot> loaded = loader.loadApiKey(keyHash);
		loaded.ifPresentOrElse(snapshot -> apiKeys.set(keyHash, snapshot),
				() -> apiKeyMisses.set(keyHash, Boolean.TRUE, negativeCacheTtlMillis, TimeUnit.MILLISECONDS));
		discardIfEvicted(generationKey, before, () -> deleteApiKeyEntries(keyHash));
		return loaded;
	}

	public void evictUser(Long userId) {
		bumpGeneration(userGeneration(userId));
		users.delete(userId);
		log.info("사용자 권한 캐시 evict: userId={}", userId);
	}

	public void evictRole(Long roleId) {
		bumpGeneration(roleGeneration(roleId));
		roles.delete(roleId);
		log.info("역할 권한 캐시 evict: roleId={}", roleId);
	}

	/** 스냅샷과 음성 캐시를 함께 지운다(발급 직후 같은 해시가 "없음"으로 남지 않게) */
	public void evictApiKey(String keyHash) {
		bumpGeneration(apiKeyGeneration(keyHash));
		deleteApiKeyEntries(keyHash);
		log.info("API Key 캐시 evict");
	}

	private void deleteApiKeyEntries(String keyHash) {
		apiKeys.delete(keyHash);
		apiKeyMisses.delete(keyHash);
	}

	/** 적재하는 동안 세대가 바뀌었으면 방금 넣은 값을 지운다(이번 요청은 읽은 값으로 처리하고, 다음 요청이 새로 적재한다) */
	private void discardIfEvicted(String generationKey, long before, Runnable discard) {
		if (generation(generationKey) != before) {
			discard.run();
			log.debug("적재 중 evict 가 끼어들어 방금 적재한 캐시 값을 버림: {}", generationKey);
		}
	}

	private long generation(String generationKey) {
		return Optional.ofNullable(generations.get(generationKey)).orElse(0L);
	}

	private void bumpGeneration(String generationKey) {
		generations.executeOnKey(generationKey, new GenerationIncrement());
	}

	private static String userGeneration(Long userId) {
		return "user:" + userId;
	}

	private static String roleGeneration(Long roleId) {
		return "role:" + roleId;
	}

	private static String apiKeyGeneration(String keyHash) {
		return "apikey:" + keyHash;
	}

	/** 세대 번호를 1 올린다(키 소유 파티션에서 원자적으로 실행) */
	static final class GenerationIncrement implements EntryProcessor<String, Long, Long>, Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

		@Override
		public Long process(Map.Entry<String, Long> entry) {
			long next = Optional.ofNullable(entry.getValue()).orElse(0L) + 1;
			entry.setValue(next);
			return next;
		}
	}
}
