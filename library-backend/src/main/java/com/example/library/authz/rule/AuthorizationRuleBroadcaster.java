package com.example.library.authz.rule;

import com.example.library.authz.cache.CacheNames;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 인가 규칙(action_urls) 변경을 클러스터 전체에 전파한다.
 * 규칙 테이블은 노드 메모리에 있으므로, 변경 노드는 커밋 후 즉시(동기) 다시 적재하고
 * 다른 노드에는 Hazelcast 토픽으로 알려 각자 다시 적재하게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationRuleBroadcaster {

	private static final String RELOAD = "reload";

	private final HazelcastInstance hazelcast;
	private final AuthorizationRuleRegistry registry;

	private ITopic<String> topic;

	@PostConstruct
	void subscribe() {
		topic = hazelcast.getTopic(CacheNames.AUTHZ_RELOAD_TOPIC);
		topic.addMessageListener(this::onMessage);
	}

	/** 규칙 변경 트랜잭션 커밋 후 호출된다 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onRulesChanged(AuthorizationRulesChangedEvent event) {
		reloadAndBroadcast();
	}

	/** 로컬 즉시 재적재 + 다른 노드 알림 */
	public void reloadAndBroadcast() {
		registry.reload();
		topic.publish(RELOAD);
	}

	private void onMessage(Message<String> message) {
		if (message.getPublishingMember() != null && message.getPublishingMember().localMember()) {
			return;
		}
		log.info("다른 노드의 인가 규칙 변경 알림 수신 — 재적재");
		registry.reload();
	}

	/** 인가 규칙 변경 이벤트 */
	public record AuthorizationRulesChangedEvent() {
	}
}
