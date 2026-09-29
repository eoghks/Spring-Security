package com.example.library.authz.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.example.library.authz.rule.AuthorizationRuleBroadcaster;
import com.example.library.authz.rule.AuthorizationRuleRegistry;
import com.example.library.config.ApiKeyProtectionProperties;
import com.example.library.config.HazelcastConfig;
import com.example.library.config.HazelcastProperties;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Hazelcast 멤버 두 개(노드 A·B)를 같은 JVM 에 띄워 분산 캐시 동작을 검증한다.
 * 애플리케이션과 같은 설정(HazelcastConfig.createConfig — 멀티캐스트 끔, TCP/IP 멤버 목록)을 쓴다.
 */
class TwoNodeAuthzCacheTest {

	private static final long ROLE_ID = 3L;
	private static final ApiKeyProtectionProperties PROTECTION = new ApiKeyProtectionProperties(Duration.ofSeconds(60), 20);

	private static HazelcastInstance nodeA;
	private static HazelcastInstance nodeB;

	@BeforeAll
	static void startCluster() {
		HazelcastProperties properties = new HazelcastProperties("two-node-test-" + UUID.randomUUID(), 5951,
				List.of("127.0.0.1"), 600, "127.0.0.1");
		nodeA = Hazelcast.newHazelcastInstance(HazelcastConfig.createConfig(properties));
		nodeB = Hazelcast.newHazelcastInstance(HazelcastConfig.createConfig(properties));
	}

	@AfterAll
	static void stopCluster() {
		nodeB.shutdown();
		nodeA.shutdown();
	}

	@Test
	@DisplayName("두 멤버가 TCP/IP 로 한 클러스터를 이룬다")
	void clusterFormed() {
		assertThat(nodeA.getCluster().getMembers()).hasSize(2);
		assertThat(nodeB.getCluster().getMembers()).hasSize(2);
	}

	@Test
	@DisplayName("노드 A 에서 역할 권한을 바꿔 evict 하면 노드 B 의 다음 조회에 즉시 반영된다")
	void evictOnNodeAReflectsOnNodeB() {
		AuthzSnapshotLoader loader = mock(AuthzSnapshotLoader.class);
		AuthzCache cacheA = new AuthzCache(nodeA, loader, PROTECTION);
		AuthzCache cacheB = new AuthzCache(nodeB, loader, PROTECTION);
		given(loader.loadRoleActions(ROLE_ID)).willReturn(new RoleActionsSnapshot(ROLE_ID, Set.of("BOOK:READ")));

		assertThat(cacheB.roleActionCodes(ROLE_ID)).containsExactly("BOOK:READ");

		// DB 에서 권한이 바뀌었지만 아직 evict 전 — 두 노드 모두 공유 캐시의 옛 값을 본다
		given(loader.loadRoleActions(ROLE_ID))
				.willReturn(new RoleActionsSnapshot(ROLE_ID, Set.of("BOOK:READ", "DASHBOARD:READ")));
		assertThat(cacheA.roleActionCodes(ROLE_ID)).containsExactly("BOOK:READ");

		// 노드 A 에서 evict(관리자 저장 커밋 후) → 노드 B 가 곧바로 새 값을 읽는다
		cacheA.evictRole(ROLE_ID);
		assertThat(cacheB.roleActionCodes(ROLE_ID)).containsExactlyInAnyOrder("BOOK:READ", "DASHBOARD:READ");
	}

	@Test
	@DisplayName("노드 A 에서 URL 매핑을 바꾸면 토픽으로 노드 B 의 규칙이 다시 적재된다")
	void ruleReloadBroadcast() {
		AuthorizationRuleRegistry registryA = mock(AuthorizationRuleRegistry.class);
		AuthorizationRuleRegistry registryB = mock(AuthorizationRuleRegistry.class);
		AuthorizationRuleBroadcaster broadcasterA = subscribed(new AuthorizationRuleBroadcaster(nodeA, registryA));
		subscribed(new AuthorizationRuleBroadcaster(nodeB, registryB));

		broadcasterA.reloadAndBroadcast();

		verify(registryA, times(1)).reload();
		verify(registryB, timeout(5000).times(1)).reload();
	}

	/** 스프링 없이 쓰므로 @PostConstruct 구독 메서드를 직접 호출한다 */
	private AuthorizationRuleBroadcaster subscribed(AuthorizationRuleBroadcaster broadcaster) {
		broadcaster.subscribe();
		return broadcaster;
	}
}
