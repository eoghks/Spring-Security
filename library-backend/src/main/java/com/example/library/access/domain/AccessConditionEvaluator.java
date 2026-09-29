package com.example.library.access.domain;

import com.example.library.common.net.IpPatterns;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * 접속 조건 판정(순수 함수). 위반이면 사유(로그 전용)를, 통과면 빈 값을 돌려준다.
 * <ul>
 *   <li>IP: 목록 중 하나와 일치(단일 IP·CIDR)</li>
 *   <li>기간: valid_from ≤ 오늘 ≤ valid_to (한쪽만 있으면 그쪽만 검사)</li>
 *   <li>요일: 허용 요일 목록에 포함. 자정을 넘는 시간 구간의 자정 이후 부분(00:00~end)은 시작한 날(전날) 요일로 본다</li>
 *   <li>시간: 분 단위로 start ≤ 현재 ≤ end(종료 18:00 이면 18:00:59 까지 허용).
 *       start 가 end 보다 늦으면 자정을 넘는 구간(예: 22:00~06:00). start == end 는 저장 단계에서 거부한다</li>
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
		if (!condition.allowedDays().isEmpty() && !condition.allowedDays().contains(effectiveDay(condition, now))) {
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

	/** 자정을 넘는 구간의 자정 이후 부분(00:00~종료)이면 전날 시작한 구간으로 보고 전날 요일을 돌려준다 */
	private static DayOfWeek effectiveDay(AccessConditionSnapshot condition, LocalDateTime now) {
		boolean earlyPartOfOvernight = isOvernight(condition)
				&& !toMinute(now.toLocalTime()).isAfter(condition.endTime().orElseThrow());
		return earlyPartOfOvernight ? now.getDayOfWeek().minus(1) : now.getDayOfWeek();
	}

	/** 종료 시각은 그 1분 전체를 포함하도록 분 단위로 비교한다(HH:mm 으로 입력받으므로) */
	private static boolean withinTime(AccessConditionSnapshot condition, LocalTime now) {
		Optional<LocalTime> start = condition.startTime();
		Optional<LocalTime> end = condition.endTime();
		LocalTime minute = toMinute(now);
		if (isOvernight(condition)) {
			return !now.isBefore(start.orElseThrow()) || !minute.isAfter(end.orElseThrow());
		}
		boolean afterStart = start.map(s -> !now.isBefore(s)).orElse(true);
		boolean beforeEnd = end.map(e -> !minute.isAfter(e)).orElse(true);
		return afterStart && beforeEnd;
	}

	private static boolean isOvernight(AccessConditionSnapshot condition) {
		return condition.startTime().isPresent() && condition.endTime().isPresent()
				&& condition.startTime().get().isAfter(condition.endTime().get());
	}

	private static LocalTime toMinute(LocalTime time) {
		return time.truncatedTo(ChronoUnit.MINUTES);
	}
}
