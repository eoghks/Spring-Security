package com.example.library.authz.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 불변 인가 규칙 테이블.
 * 메서드별로 규칙을 모으고 구체적인 패턴이 앞에 오도록(PathPattern.SPECIFICITY_COMPARATOR) 정렬해 둔다.
 * 요청과 일치하는 첫 규칙(가장 구체적인 규칙)만 판정에 쓴다. 일치 규칙이 없으면 호출자는 거부해야 한다(fail-closed).
 */
public final class UrlRuleTable {

	private static final Comparator<UrlRule> SPECIFICITY =
			Comparator.comparing(UrlRule::pathPattern, PathPattern.SPECIFICITY_COMPARATOR);

	private final Map<String, List<UrlRule>> rulesByMethod;

	private UrlRuleTable(Map<String, List<UrlRule>> rulesByMethod) {
		this.rulesByMethod = rulesByMethod;
	}

	public static UrlRuleTable empty() {
		return new UrlRuleTable(Map.of());
	}

	/** 원천 행 목록으로 규칙 테이블을 만든다. 같은 (메서드, 패턴)은 한 규칙으로 합친다 */
	public static UrlRuleTable of(Collection<UrlRuleSource> sources) {
		PathPatternParser parser = new PathPatternParser();
		Map<String, List<UrlRuleSource>> grouped = sources.stream()
				.collect(Collectors.groupingBy(UrlRuleTable::keyOf, LinkedHashMap::new, Collectors.toList()));
		List<UrlRule> rules = grouped.values().stream()
				.map(group -> merge(group, parser))
				.toList();
		Map<String, List<UrlRule>> byMethod = rules.stream()
				.collect(Collectors.groupingBy(UrlRule::httpMethod, Collectors.collectingAndThen(Collectors.toList(),
						list -> list.stream().sorted(SPECIFICITY).toList())));
		return new UrlRuleTable(Map.copyOf(byMethod));
	}

	private static String keyOf(UrlRuleSource source) {
		return normalizeMethod(source.httpMethod()) + " " + source.urlPattern();
	}

	private static UrlRule merge(List<UrlRuleSource> group, PathPatternParser parser) {
		UrlRuleSource first = group.getFirst();
		Set<String> actionCodes = new HashSet<>();
		boolean authenticatedOnly = false;
		for (UrlRuleSource source : group) {
			source.actionCode().ifPresent(actionCodes::add);
			authenticatedOnly |= source.actionCode().isEmpty();
		}
		return new UrlRule(normalizeMethod(first.httpMethod()), parser.parse(first.urlPattern()), actionCodes,
				authenticatedOnly);
	}

	private static String normalizeMethod(String method) {
		return method.toUpperCase(Locale.ROOT);
	}

	/** 요청(메서드, 애플리케이션 내부 경로)에 가장 구체적으로 일치하는 규칙 */
	public Optional<UrlRule> match(String httpMethod, String path) {
		PathContainer container = PathContainer.parsePath(path);
		return rulesByMethod.getOrDefault(normalizeMethod(httpMethod), List.of()).stream()
				.filter(rule -> rule.pathPattern().matches(container))
				.findFirst();
	}

	/** 보유 액션으로 호출 가능한 action_urls 규칙의 "METHOD /pattern" 집합(정렬, 중복 제거) */
	public Set<String> callableUrls(Set<String> grantedActionCodes) {
		return allRules().stream()
				.filter(rule -> rule.grantsAny(grantedActionCodes))
				.map(UrlRule::key)
				.collect(Collectors.toCollection(TreeSet::new));
	}

	public List<UrlRule> allRules() {
		List<UrlRule> all = new ArrayList<>();
		rulesByMethod.values().forEach(all::addAll);
		return List.copyOf(all);
	}

	public int size() {
		return rulesByMethod.values().stream().mapToInt(List::size).sum();
	}
}
