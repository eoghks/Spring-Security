package com.example.library.apikey.api;

import com.example.library.common.net.IpOrCidr;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * API Key 발급 요청.
 *
 * @param name       용도 이름
 * @param actionIds  부여할 액션 ID(발급자 본인이 보유한 액션만 가능)
 * @param allowedIps 허용 IP·CIDR(비우면 전체 허용)
 * @param expiresAt  만료 시각(없으면 만료 없음)
 */
public record ApiKeyIssueRequest(
		@NotBlank(message = "이름을 입력하세요.") @Size(max = 100, message = "이름은 100자 이하입니다.")
		String name,

		@NotEmpty(message = "부여할 액션을 하나 이상 선택하세요.")
		List<@NotNull Long> actionIds,

		@NotNull(message = "허용 IP 목록이 필요합니다(비우면 전체 허용).")
		@Size(max = 50, message = "허용 IP 는 50개 이하입니다.")
		List<@IpOrCidr String> allowedIps,

		@Future(message = "만료 시각은 미래여야 합니다.")
		LocalDateTime expiresAt) {
}
