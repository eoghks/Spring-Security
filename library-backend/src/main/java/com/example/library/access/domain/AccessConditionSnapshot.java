package com.example.library.access.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 캐시에 담는 접속 조건 값 객체(불변, 직렬화 가능).
 * Optional 은 직렬화되지 않으므로 필드는 nullable 로 두고 접근자에서 Optional 로 감싼다.
 */
@ToString
@EqualsAndHashCode
public final class AccessConditionSnapshot implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private static final AccessConditionSnapshot UNRESTRICTED = builder().build();

	private final List<String> allowedIps;
	private final List<DayOfWeek> allowedDays;
	private final LocalDate validFrom;
	private final LocalDate validTo;
	private final LocalTime startTime;
	private final LocalTime endTime;

	@Builder
	private AccessConditionSnapshot(List<String> allowedIps, List<DayOfWeek> allowedDays, LocalDate validFrom,
			LocalDate validTo, LocalTime startTime, LocalTime endTime) {
		this.allowedIps = allowedIps == null ? List.of() : List.copyOf(allowedIps);
		this.allowedDays = allowedDays == null ? List.of() : List.copyOf(allowedDays);
		this.validFrom = validFrom;
		this.validTo = validTo;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	/** 제한 없음(조건 미설정 사용자) */
	public static AccessConditionSnapshot unrestricted() {
		return UNRESTRICTED;
	}

	public List<String> allowedIps() {
		return allowedIps;
	}

	public List<DayOfWeek> allowedDays() {
		return allowedDays;
	}

	public Optional<LocalDate> validFrom() {
		return Optional.ofNullable(validFrom);
	}

	public Optional<LocalDate> validTo() {
		return Optional.ofNullable(validTo);
	}

	public Optional<LocalTime> startTime() {
		return Optional.ofNullable(startTime);
	}

	public Optional<LocalTime> endTime() {
		return Optional.ofNullable(endTime);
	}
}
