package com.example.library.user.api;

import jakarta.validation.constraints.NotNull;

/**
 * 역할 변경 요청.
 */
public record ChangeRoleRequest(@NotNull(message = "역할을 선택하세요.") Long roleId) {
}
