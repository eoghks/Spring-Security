package com.example.library.config;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

/**
 * Hazelcast(embedded) 설정.
 * 오픈소스 Hazelcast 는 멤버·클라이언트 인증이 없으므로 바인딩 인터페이스를 루프백 또는 사설 대역으로만 허용한다.
 *
 * @param clusterName       클러스터 이름(같은 이름끼리만 합류)
 * @param port              멤버 포트(사용 중이면 자동 증가)
 * @param members           TCP/IP 멤버 주소 목록
 * @param timeToLiveSeconds 캐시 항목 최대 수명(evict 누락 대비 안전장치)
 * @param networkInterface  멤버가 바인딩할 인터페이스(app.hazelcast.interface). Hazelcast 와일드카드(10.0.0.*)·범위(10.0.0.1-9) 허용
 */
@ConfigurationProperties(prefix = "app.hazelcast")
public record HazelcastProperties(String clusterName, int port, List<String> members, int timeToLiveSeconds,
		@Name("interface") String networkInterface) {

	/** 루프백(127/8)·사설 대역(10/8, 172.16/12, 192.168/16) — 나머지 옥텟은 숫자·*·범위 표기 */
	private static final Pattern PRIVATE_INTERFACE = Pattern.compile(
			"^(127(\\.[\\d*-]+){3}|10(\\.[\\d*-]+){3}|192\\.168(\\.[\\d*-]+){2}|172\\.(1[6-9]|2\\d|3[01])(\\.[\\d*-]+){2})$");

	public HazelcastProperties {
		members = members == null ? List.of() : List.copyOf(members);
		if (networkInterface == null || !PRIVATE_INTERFACE.matcher(networkInterface.trim()).matches()) {
			throw new IllegalStateException("Hazelcast 바인딩 인터페이스(app.hazelcast.interface)는 루프백 또는 사설 대역만 "
					+ "허용합니다(멤버 인증이 없으므로 공개 인터페이스에 열지 않는다): " + networkInterface);
		}
		networkInterface = networkInterface.trim();
	}
}
