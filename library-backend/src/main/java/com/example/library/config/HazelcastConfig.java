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
 */
@Configuration
public class HazelcastConfig {

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
		configureNetwork(config.getNetworkConfig(), properties);
		for (String mapName : CacheNames.MAPS) {
			config.addMapConfig(new MapConfig(mapName).setTimeToLiveSeconds(properties.timeToLiveSeconds()));
		}
		return config;
	}

	private static void configureNetwork(NetworkConfig network, HazelcastProperties properties) {
		network.setPort(properties.port()).setPortAutoIncrement(true);
		JoinConfig join = network.getJoin();
		join.getMulticastConfig().setEnabled(false);
		join.getAutoDetectionConfig().setEnabled(false);
		join.getTcpIpConfig().setEnabled(true).setConnectionTimeoutSeconds(1).setMembers(properties.members());
	}
}
