package com.example.library.authz.api;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 역할 부여 액션 전체 교체 요청.
 */
public record RoleActionsRequest(@NotNull(message = "액션 목록이 필요합니다.") List<@NotNull Long> actionIds) {
}
