package com.example.library.authz.application;

import com.example.library.authz.domain.Role;
import com.example.library.authz.domain.RoleRepository;
import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import com.example.library.security.UserPrincipal;
import com.example.library.user.domain.UserRepository;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 권한 관리 기능(역할 권한 저장·액션 URL 편집·역할 변경)의 권한 상승 방지 규칙.
 * <ul>
 *   <li>부여 가능한 액션은 행위자가 보유한 액션 이내 — API Key 발급과 같은 규칙</li>
 *   <li>행위자는 자기 역할의 권한·액션 URL 을 바꿀 수 없다(관리자 역할은 이미 모든 액션을 가지므로 URL 편집만 허용)</li>
 *   <li>활성(잠기지 않은) 관리자가 한 명뿐이면 그 계정의 역할을 바꾸거나 잠그지 않는다</li>
 * </ul>
 * 호출하는 서비스의 트랜잭션 안에서 쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrantGuard {

	private final RoleRepository roleRepository;
	private final UserRepository userRepository;

	/** 행위자의 현재 역할(DB 기준) */
	public Role actorRole(UserPrincipal actor) {
		return roleRepository.findById(actor.roleId())
				.orElseThrow(() -> new BusinessException(ErrorCode.ACCESS_DENIED));
	}

	/** 부여하려는 액션을 행위자 역할이 모두 보유해야 한다 */
	public void ensureOwned(Role actorRole, Collection<Long> actionIds) {
		if (!actorRole.getActionIds().containsAll(actionIds)) {
			throw new BusinessException(ErrorCode.ACTION_NOT_OWNED);
		}
	}

	/** 자기 역할(또는 자기 역할이 보유한 액션)을 대상으로 한 변경을 막는다 */
	public void ensureNotOwnRole(Role actorRole, Long targetRoleId) {
		if (actorRole.getId().equals(targetRoleId)) {
			throw new BusinessException(ErrorCode.CANNOT_EDIT_OWN_ROLE);
		}
	}

	/** 자기 역할이 보유한 액션의 URL 편집을 막는다. 관리자 역할은 이미 모든 액션을 가져 상승 여지가 없으므로 허용한다 */
	public void ensureNotOwnAction(Role actorRole, Long actionId) {
		if (!actorRole.isSystemAdmin() && actorRole.getActionIds().contains(actionId)) {
			throw new BusinessException(ErrorCode.CANNOT_EDIT_OWN_ROLE);
		}
	}

	/**
	 * 대상 사용자가 마지막 활성 관리자인지 판정한다.
	 * 관리자 역할 행을 잠근 뒤 세어, 역할 변경·자동 잠금이 동시에 일어나도 판정이 엇갈리지 않게 한다.
	 */
	public boolean isLastActiveAdmin(Role targetRole, Long targetUserId) {
		if (!targetRole.isSystemAdmin()) {
			return false;
		}
		roleRepository.findForUpdate(targetRole.getId());
		return userRepository.countActiveByRoleExcluding(targetRole.getId(), targetUserId) == 0;
	}
}
