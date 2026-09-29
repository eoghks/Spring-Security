package com.example.library.authz.cache;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 역할이 보유한 액션 코드("메뉴코드:액션코드") 집합.
 */
public record RoleActionsSnapshot(Long roleId, Set<String> actionCodes) implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	public RoleActionsSnapshot {
		actionCodes = Set.copyOf(actionCodes);
	}
}
