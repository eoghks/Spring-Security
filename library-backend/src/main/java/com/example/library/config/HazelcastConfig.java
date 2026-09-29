package com.example.library.config;

import com.example.library.authz.cache.CacheNames;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NetworkConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 권한 캐시용 Hazelcast 멤버 설정. Config 빈만 두면 Spring Boot 가 HazelcastInstance 를 만든다.
 * 멀티캐스트 대신 TCP/IP 멤버 목록으로 합류하므로 단일 노드에서도 그대로 뜬다.
 * 멤버 인증이 없는 오픈소스 Hazelcast 이므로 지정한 인터페이스(기본 루프백)에만 바인딩한다.
 */
@Configuration
public class HazelcastConfig {

	/** 분 단위 실패 버킷은 해당 분이 지나면 쓸모없으므로 넉넉히 2분 뒤 사라지게 한다 */
	private static final int FAILURE_BUCKET_TTL_SECONDS = 120;

	@Bean
	public Config hazelcastMemberConfig(HazelcastProperties properties) {
		return createConfig(properties);
	}

	/** 테스트에서 다중 멤버를 띄울 때도 같은 설정을 쓰도록 정적 메서드로 분리한다 */
	public static Config createConfig(HazelcastProperties properties) {
		Config config = new Config();
		config.setClusterName(properties.clusterName());
		config.setProperty("hazelcast.phone.home.enabled", "false");
		config.setProperty("hazelcast.logging.type", "slf4j");
		config.setProperty("hazelcast.wait.seconds.before.join", "0");
		// 모든 인터페이스(0.0.0.0)가 아니라 지정한 인터페이스에만 바인딩한다
		config.setProperty("hazelcast.socket.bind.any", "false");
		configureNetwork(config.getNetworkConfig(), properties);
		for (String mapName : CacheNames.MAPS) {
			config.addMapConfig(new MapConfig(mapName).setTimeToLiveSeconds(properties.timeToLiveSeconds()));
		}
		config.addMapConfig(new MapConfig(CacheNames.API_KEY_FAILURES).setTimeToLiveSeconds(FAILURE_BUCKET_TTL_SECONDS));
		return config;
	}

	private static void configureNetwork(NetworkConfig network, HazelcastProperties properties) {
		network.setPort(properties.port()).setPortAutoIncrement(true);
		network.getInterfaces().setEnabled(true).addInterface(properties.networkInterface());
		JoinConfig join = network.getJoin();
		join.getMulticastConfig().setEnabled(false);
		join.getAutoDetectionConfig().setEnabled(false);
		join.getTcpIpConfig().setEnabled(true).setConnectionTimeoutSeconds(1).setMembers(properties.members());
	}
}
