package com.example.library.authz.api;

import java.util.List;

/**
 * 역할 보유 액션 ID 목록.
 */
public record RoleActionsResponse(Long roleId, List<Long> actionIds) {
}
