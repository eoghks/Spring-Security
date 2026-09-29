package com.example.library.authz.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.library.config.ApiKeyProtectionProperties;
import com.example.library.config.HazelcastConfig;
import com.example.library.config.HazelcastProperties;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 존재하지 않는 API Key 해시의 음성 캐시 동작(실제 Hazelcast 멤버 + DB 적재기 목).
 */
class AuthzCacheNegativeCacheTest {

	private static HazelcastInstance hazelcast;

	private AuthzSnapshotLoader loader;
	private String keyHash;

	@BeforeAll
	static void startMember() {
		HazelcastProperties properties = new HazelcastProperties("negative-cache-test-" + UUID.randomUUID(), 5971,
				List.of("127.0.0.1"), 600, "127.0.0.1");
		hazelcast = Hazelcast.newHazelcastInstance(HazelcastConfig.createConfig(properties));
	}

	@AfterAll
	static void stopMember() {
		hazelcast.shutdown();
	}

	@BeforeEach
	void setUp() {
		loader = mock(AuthzSnapshotLoader.class);
		keyHash = UUID.randomUUID().toString();
		given(loader.loadApiKey(keyHash)).willReturn(Optional.empty());
	}

	@Test
	@DisplayName("없는 해시는 한 번만 DB 를 조회하고 TTL 동안은 음성 캐시로 빈 값을 돌려준다")
	void missIsCached() {
		AuthzCache cache = cacheWithTtl(Duration.ofSeconds(60));

		assertThat(cache.findApiKey(keyHash)).isEmpty();
		assertThat(cache.findApiKey(keyHash)).isEmpty();
		assertThat(cache.findApiKey(keyHash)).isEmpty();

		verify(loader, times(1)).loadApiKey(keyHash);
	}

	@Test
	@DisplayName("evict(발급·폐기 커밋 후)하면 음성 캐시가 지워져 다음 조회는 DB 에서 새 키를 읽는다")
	void evictClearsMiss() {
		AuthzCache cache = cacheWithTtl(Duration.ofSeconds(60));
		assertThat(cache.findApiKey(keyHash)).isEmpty();

		ApiKeySnapshot issued = new ApiKeySnapshot(1L, "새 키", false, null, Set.of("BOOK:READ"), Set.of());
		given(loader.loadApiKey(keyHash)).willReturn(Optional.of(issued));
		cache.evictApiKey(keyHash);

		assertThat(cache.findApiKey(keyHash)).contains(issued);
		verify(loader, times(2)).loadApiKey(keyHash);
	}

	@Test
	@DisplayName("음성 캐시 TTL 이 지나면 다시 DB 를 조회한다")
	void missExpires() {
		AuthzCache cache = cacheWithTtl(Duration.ofSeconds(1));
		assertThat(cache.findApiKey(keyHash)).isEmpty();

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			cache.findApiKey(keyHash);
			verify(loader, times(2)).loadApiKey(keyHash);
		});
	}

	private AuthzCache cacheWithTtl(Duration ttl) {
		return new AuthzCache(hazelcast, loader, new ApiKeyProtectionProperties(ttl, 20));
	}
}
