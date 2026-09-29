package com.example.library.access.domain;

import com.example.library.common.net.IpPatterns;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * 접속 조건 판정(순수 함수). 위반이면 사유(로그 전용)를, 통과면 빈 값을 돌려준다.
 * <ul>
 *   <li>IP: 목록 중 하나와 일치(단일 IP·CIDR)</li>
 *   <li>기간: valid_from ≤ 오늘 ≤ valid_to (한쪽만 있으면 그쪽만 검사)</li>
 *   <li>요일: 허용 요일 목록에 포함</li>
 *   <li>시간: start ≤ 현재 ≤ end, start 가 end 보다 늦으면 자정을 넘는 구간(예: 22:00~06:00)</li>
 * </ul>
 */
public final class AccessConditionEvaluator {

	private AccessConditionEvaluator() {
	}

	public static Optional<String> findViolation(AccessConditionSnapshot condition, String clientIp,
			LocalDateTime now) {
		if (!IpPatterns.matchesAny(condition.allowedIps(), clientIp)) {
			return Optional.of("허용되지 않은 IP");
		}
		if (!withinPeriod(condition, now.toLocalDate())) {
			return Optional.of("허용 기간 밖");
		}
		if (!condition.allowedDays().isEmpty() && !condition.allowedDays().contains(now.getDayOfWeek())) {
			return Optional.of("허용되지 않은 요일");
		}
		if (!withinTime(condition, now.toLocalTime())) {
			return Optional.of("허용 시간대 밖");
		}
		return Optional.empty();
	}

	private static boolean withinPeriod(AccessConditionSnapshot condition, LocalDate today) {
		boolean afterStart = condition.validFrom().map(from -> !today.isBefore(from)).orElse(true);
		boolean beforeEnd = condition.validTo().map(to -> !today.isAfter(to)).orElse(true);
		return afterStart && beforeEnd;
	}

	private static boolean withinTime(AccessConditionSnapshot condition, LocalTime now) {
		Optional<LocalTime> start = condition.startTime();
		Optional<LocalTime> end = condition.endTime();
		if (start.isPresent() && end.isPresent() && start.get().isAfter(end.get())) {
			// 자정을 넘는 구간
			return !now.isBefore(start.get()) || !now.isAfter(end.get());
		}
		boolean afterStart = start.map(s -> !now.isBefore(s)).orElse(true);
		boolean beforeEnd = end.map(e -> !now.isAfter(e)).orElse(true);
		return afterStart && beforeEnd;
	}
}
