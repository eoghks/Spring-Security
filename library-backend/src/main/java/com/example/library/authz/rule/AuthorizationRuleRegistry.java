package com.example.library.authz.rule;

import com.example.library.authz.domain.ActionUrlRepository;
import com.example.library.authz.domain.AuthenticatedUrlRepository;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * action_urls·authenticated_urls 를 읽어 만든 규칙 테이블을 보관한다.
 * 기동 시 한 번 적재하고, 관리 화면에서 URL 매핑이 바뀌면 reload() 로 통째로 교체한다(요청 처리 중에는 불변 객체만 읽는다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationRuleRegistry {

	private final ActionUrlRepository actionUrlRepository;
	private final AuthenticatedUrlRepository authenticatedUrlRepository;

	private volatile UrlRuleTable table = UrlRuleTable.empty();

	@PostConstruct
	void init() {
		reload();
	}

	/** DB 에서 규칙을 다시 읽어 교체한다 */
	@Transactional(readOnly = true)
	public synchronized void reload() {
		List<UrlRuleSource> sources = new ArrayList<>();
		actionUrlRepository.findAllWithAction().forEach(url -> sources.add(UrlRuleSource.forAction(
				url.getHttpMethod(), url.getUrlPattern(), url.getAction().authorityCode())));
		authenticatedUrlRepository.findAll().forEach(url -> sources.add(UrlRuleSource.forAuthenticated(
				url.getHttpMethod(), url.getUrlPattern())));
		table = UrlRuleTable.of(sources);
		log.info("인가 규칙 적재 완료: 원천 {}건 → 규칙 {}건", sources.size(), table.size());
	}

	public UrlRuleTable current() {
		return table;
	}
}
