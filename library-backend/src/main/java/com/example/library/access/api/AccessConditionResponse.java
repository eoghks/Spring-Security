package com.example.library.access.api;

import com.example.library.access.domain.AccessConditionSnapshot;
import com.example.library.access.domain.UserAccessCondition.AccessDays;
import com.example.library.user.domain.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 사용자별 접속 조건. 미설정이면 configured=false 이고 모든 항목이 비어 있다(제한 없음).
 */
public record AccessConditionResponse(Long userId, String username, String name, String roleCode,
		boolean configured, List<String> allowedIps, LocalDate validFrom, LocalDate validTo,
		List<String> allowedDays,
		@JsonFormat(pattern = "HH:mm") LocalTime startTime,
		@JsonFormat(pattern = "HH:mm") LocalTime endTime) {

	public static AccessConditionResponse of(User user, boolean configured, AccessConditionSnapshot condition) {
		return new AccessConditionResponse(user.getId(), user.getUsername(), user.getName(), user.getRole().getCode(),
				configured, condition.allowedIps(), condition.validFrom().orElse(null),
				condition.validTo().orElse(null),
				condition.allowedDays().stream().map(AccessDays::code).toList(),
				condition.startTime().orElse(null), condition.endTime().orElse(null));
	}
}
