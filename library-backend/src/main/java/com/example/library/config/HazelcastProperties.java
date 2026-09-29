package com.example.library.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Hazelcast(embedded) 설정.
 *
 * @param clusterName       클러스터 이름(같은 이름끼리만 합류)
 * @param port              멤버 포트(사용 중이면 자동 증가)
 * @param members           TCP/IP 멤버 주소 목록
 * @param timeToLiveSeconds 캐시 항목 최대 수명(evict 누락 대비 안전장치)
 */
@ConfigurationProperties(prefix = "app.hazelcast")
public record HazelcastProperties(String clusterName, int port, List<String> members, int timeToLiveSeconds) {
}
