package com.example.library.authz.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.library.access.domain.AccessConditionSnapshot;
import com.example.library.config.ApiKeyProtectionProperties;
import com.example.library.config.HazelcastConfig;
import com.example.library.config.HazelcastProperties;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.time.Duration;
import java.time.LocalDateTime;
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
 * 캐시 미스 적재 도중 evict(권한 변경 커밋)가 끼어들어도 옛 값이 캐시에 남지 않는지 검증한다(실제 Hazelcast 멤버 + 적재기 목).
 * 적재기 목이 DB 를 읽는 순간에 evict 를 호출해 "옛 값 읽기 → evict → 옛 값 저장" 순서를 그대로 재현한다.
 */
class AuthzCacheLoadEvictRaceTest {

	private static HazelcastInstance hazelcast;

	private AuthzSnapshotLoader loader;
	private AuthzCache cache;

	@BeforeAll
	static void startMember() {
		HazelcastProperties properties = new HazelcastProperties("race-test-" + UUID.randomUUID(), 5941,
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
		cache = new AuthzCache(hazelcast, loader, new ApiKeyProtectionProperties(Duration.ofSeconds(60), 20));
	}

	@Test
	@DisplayName("사용자 적재 중 evict 가 끼어들면 옛 스냅샷은 버려지고 다음 조회가 새 값을 읽는다")
	void userLoadRacingEvict() {
		UserAuthSnapshot stale = user(10L, false);
		UserAuthSnapshot fresh = user(10L, true);
		given(loader.loadUser(10L)).willAnswer(invocation -> {
			cache.evictUser(10L);
			return Optional.of(stale);
		});

		assertThat(cache.findUser(10L)).contains(stale);

		// 첫 번째 응답(evict 호출)이 다시 실행되지 않도록 doReturn 형태로 바꿔 둔다
		willReturn(Optional.of(fresh)).given(loader).loadUser(10L);
		assertThat(cache.findUser(10L)).contains(fresh);
	}

	@Test
	@DisplayName("역할 적재 중 evict 가 끼어들면 옛 액션 목록은 버려진다")
	void roleLoadRacingEvict() {
		given(loader.loadRoleActions(20L)).willAnswer(invocation -> {
			cache.evictRole(20L);
			return new RoleActionsSnapshot(20L, Set.of("BOOK:READ", "USER_MANAGE:READ"));
		});
		cache.roleActionCodes(20L);

		willReturn(new RoleActionsSnapshot(20L, Set.of("BOOK:READ"))).given(loader).loadRoleActions(20L);
		assertThat(cache.roleActionCodes(20L)).containsExactly("BOOK:READ");
	}

	@Test
	@DisplayName("API Key 적재 중 evict(폐기)가 끼어들면 옛 스냅샷은 버려진다")
	void apiKeyLoadRacingEvict() {
		String hash = UUID.randomUUID().toString();
		ApiKeySnapshot active = new ApiKeySnapshot(1L, "키", false, LocalDateTime.MAX, Set.of("BOOK:READ"), Set.of(), 1L);
		ApiKeySnapshot revoked = new ApiKeySnapshot(1L, "키", true, LocalDateTime.MAX, Set.of("BOOK:READ"), Set.of(), 1L);
		given(loader.loadApiKey(hash)).willAnswer(invocation -> {
			cache.evictApiKey(hash);
			return Optional.of(active);
		});
		cache.findApiKey(hash);

		willReturn(Optional.of(revoked)).given(loader).loadApiKey(hash);
		assertThat(cache.findApiKey(hash)).contains(revoked);
	}

	@Test
	@DisplayName("경합이 없으면 적재한 값이 그대로 캐시되어 DB 를 다시 읽지 않는다")
	void normalLoadIsCached() {
		given(loader.loadUser(30L)).willReturn(Optional.of(user(30L, false)));

		cache.findUser(30L);
		cache.findUser(30L);

		verify(loader, times(1)).loadUser(30L);
	}

	private UserAuthSnapshot user(Long userId, boolean locked) {
		return new UserAuthSnapshot(userId, "user" + userId, 3L, locked, AccessConditionSnapshot.unrestricted());
	}
}
