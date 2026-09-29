package com.example.library.access.api;

import com.example.library.access.domain.AccessConditionSnapshot;
import com.example.library.access.domain.UserAccessCondition.AccessDays;
import com.example.library.common.net.IpOrCidr;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 접속 조건 저장 요청. 목록이 비거나 값이 없으면 그 항목은 제한하지 않는다.
 */
public record AccessConditionRequest(
		@NotNull(message = "허용 IP 목록이 필요합니다(비우면 전체 허용).")
		@Size(max = 50, message = "허용 IP 는 50개 이하입니다.")
		List<@IpOrCidr String> allowedIps,

		LocalDate validFrom,

		LocalDate validTo,

		@NotNull(message = "허용 요일 목록이 필요합니다(비우면 전체 허용).")
		List<@Pattern(regexp = "^(MON|TUE|WED|THU|FRI|SAT|SUN)$", message = "요일은 MON~SUN 입니다.") String> allowedDays,

		@JsonFormat(pattern = "HH:mm") LocalTime startTime,

		@JsonFormat(pattern = "HH:mm") LocalTime endTime) {

	@JsonIgnore
	@AssertTrue(message = "시작일은 종료일보다 늦을 수 없습니다.")
	public boolean isPeriodValid() {
		return validFrom == null || validTo == null || !validFrom.isAfter(validTo);
	}

	/** 시작 = 종료면 그 1분만 허용되는 뜻밖의 조건이 되므로 거부한다(하루 종일이면 시간을 비운다) */
	@JsonIgnore
	@AssertTrue(message = "시작 시각과 종료 시각이 같을 수 없습니다(하루 종일이면 비워 두세요).")
	public boolean isTimeRangeValid() {
		return startTime == null || endTime == null || !startTime.equals(endTime);
	}

	public AccessConditionSnapshot toSnapshot() {
		return AccessConditionSnapshot.builder()
				.allowedIps(allowedIps.stream().map(String::trim).distinct().toList())
				.allowedDays(allowedDays.stream().distinct().map(AccessDays::parse).toList())
				.validFrom(validFrom)
				.validTo(validTo)
				.startTime(startTime)
				.endTime(endTime)
				.build();
	}
}
