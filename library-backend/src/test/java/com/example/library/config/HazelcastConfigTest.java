package com.example.library.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Hazelcast 네트워크 노출 제한 검증 — 멤버 인증이 없으므로 지정한 사설·루프백 인터페이스에만 바인딩해야 한다.
 */
class HazelcastConfigTest {

	@Test
	@DisplayName("기본 설정은 루프백에만 바인딩하고 멀티캐스트·자동 탐지를 끈다")
	void bindsOnlyToConfiguredInterface() {
		Config config = HazelcastConfig.createConfig(properties("127.0.0.1"));

		assertThat(config.getProperty("hazelcast.socket.bind.any")).isEqualTo("false");
		assertThat(config.getNetworkConfig().getInterfaces().isEnabled()).isTrue();
		assertThat(config.getNetworkConfig().getInterfaces().getInterfaces()).containsExactly("127.0.0.1");
		JoinConfig join = config.getNetworkConfig().getJoin();
		assertThat(join.getMulticastConfig().isEnabled()).isFalse();
		assertThat(join.getAutoDetectionConfig().isEnabled()).isFalse();
	}

	@Test
	@DisplayName("실제로 띄운 멤버의 주소가 루프백이다")
	void startedMemberUsesLoopback() {
		HazelcastInstance instance = Hazelcast.newHazelcastInstance(HazelcastConfig.createConfig(properties("127.0.0.1")));
		String host = instance.getCluster().getLocalMember().getAddress().getHost();
		instance.shutdown();

		assertThat(host).isEqualTo("127.0.0.1");
	}

	@ParameterizedTest
	@ValueSource(strings = {"127.0.0.1", "10.0.0.*", "10.0.0.1-9", "172.16.5.4", "172.31.0.*", "192.168.1.10"})
	@DisplayName("루프백·사설 대역 인터페이스는 허용한다")
	void acceptsPrivateInterfaces(String networkInterface) {
		assertThat(properties(networkInterface).networkInterface()).isEqualTo(networkInterface);
	}

	@ParameterizedTest
	@ValueSource(strings = {"0.0.0.0", "*.*.*.*", "8.8.8.8", "172.32.0.1", "192.169.0.1", "11.0.0.1", " "})
	@DisplayName("모든 인터페이스·공개 대역 인터페이스는 기동 시 거부한다")
	void rejectsPublicInterfaces(String networkInterface) {
		assertThatThrownBy(() -> properties(networkInterface))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("app.hazelcast.interface");
	}

	private HazelcastProperties properties(String networkInterface) {
		return new HazelcastProperties("config-test-" + UUID.randomUUID(), 5991, List.of("127.0.0.1"), 600,
				networkInterface);
	}
}
