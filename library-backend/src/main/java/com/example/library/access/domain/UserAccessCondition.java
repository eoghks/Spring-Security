package com.example.library.access.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 접속 조건. 각 항목이 비어 있으면 그 항목은 제한하지 않는다.
 * allowed_ips 는 콤마 구분(단일 IP 또는 CIDR), allowed_days 는 "MON,TUE" 형식이다.
 */
@Entity
@Table(name = "user_access_conditions")
@Getter(AccessLevel.NONE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccessCondition {

	@Id
	@Getter
	private Long userId;

	private String allowedIps;

	private LocalDate validFrom;

	private LocalDate validTo;

	private String allowedDays;

	private LocalTime startTime;

	private LocalTime endTime;

	@Getter
	private LocalDateTime updatedAt;

	public UserAccessCondition(Long userId) {
		this.userId = userId;
	}

	/** 조건 전체를 교체한다 */
	public void update(AccessConditionSnapshot condition, LocalDateTime now) {
		this.allowedIps = String.join(",", condition.allowedIps());
		this.allowedDays = String.join(",", condition.allowedDays().stream().map(AccessDays::code).toList());
		this.validFrom = condition.validFrom().orElse(null);
		this.validTo = condition.validTo().orElse(null);
		this.startTime = condition.startTime().orElse(null);
		this.endTime = condition.endTime().orElse(null);
		this.updatedAt = now;
	}

	public AccessConditionSnapshot toSnapshot() {
		return AccessConditionSnapshot.builder()
				.allowedIps(splitCsv(allowedIps))
				.allowedDays(splitCsv(allowedDays).stream().map(AccessDays::parse).toList())
				.validFrom(validFrom)
				.validTo(validTo)
				.startTime(startTime)
				.endTime(endTime)
				.build();
	}

	private static List<String> splitCsv(String csv) {
		return Optional.ofNullable(csv).stream()
				.flatMap(value -> Arrays.stream(value.split(",")))
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.toList();
	}

	/** 요일 코드(MON~SUN) 변환 */
	public static final class AccessDays {

		private AccessDays() {
		}

		public static String code(DayOfWeek day) {
			return day.name().substring(0, 3);
		}

		public static DayOfWeek parse(String code) {
			return Arrays.stream(DayOfWeek.values())
					.filter(day -> code(day).equalsIgnoreCase(code))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException("알 수 없는 요일 코드: " + code));
		}
	}
}
