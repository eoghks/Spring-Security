package com.example.library.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 통합 테스트 공통 설정.
 * 모든 통합 테스트가 같은 설정을 공유해 스프링 컨텍스트(와 Hazelcast 멤버)를 하나만 띄운다.
 * 클러스터 이름을 매번 무작위로 정해 로컬에서 실행 중인 개발 서버 클러스터에 합류하지 않게 한다.
 */
@SpringBootTest(properties = {
		"app.hazelcast.cluster-name=library-test-${random.uuid}",
		"app.hazelcast.port=5901"
})
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {
}
