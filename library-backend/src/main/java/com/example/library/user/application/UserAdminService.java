package com.example.library.user.application;

import com.example.library.authz.application.GrantGuard;
import com.example.library.authz.cache.AuthzChangedEvent;
import com.example.library.authz.domain.Role;
import com.example.library.authz.domain.RoleRepository;
import com.example.library.common.api.LikePatterns;
import com.example.library.common.api.PageResponse;
import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import com.example.library.security.UserPrincipal;
import com.example.library.user.api.UserAdminResponse;
import com.example.library.user.domain.User;
import com.example.library.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 관리(목록·역할 변경·잠금 해제). 변경 후 커밋 시점에 해당 사용자 캐시를 evict 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserAdminService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final GrantGuard grantGuard;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional(readOnly = true)
	public PageResponse<UserAdminResponse> search(String keyword, int page, int size) {
		PageRequest pageable = PageRequest.of(Math.max(page, 0), PageResponse.clampSize(size), Sort.by("id"));
		return PageResponse.of(userRepository.search(LikePatterns.escape(keyword.trim()), pageable), UserAdminResponse::from);
	}

	/**
	 * 역할 변경. 관리자가 자기 역할을 바꿔 스스로 권한을 잃는 사고를 막는다.
	 * 권한 상승 방지: 대상의 현재 역할과 새 역할의 액션을 행위자가 모두 보유해야 한다(예: 관리자 아닌 사람은 ADMIN 부여 불가).
	 * 마지막 활성 관리자의 역할은 바꿀 수 없다(409).
	 */
	public UserAdminResponse changeRole(UserPrincipal actor, Long userId, Long roleId) {
		if (actor.userId().equals(userId)) {
			throw new BusinessException(ErrorCode.CANNOT_CHANGE_OWN_ROLE);
		}
		User user = findUser(userId);
		Role role = roleRepository.findById(roleId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
		Role actorRole = grantGuard.actorRole(actor);
		grantGuard.ensureOwned(actorRole, user.getRole().getActionIds());
		grantGuard.ensureOwned(actorRole, role.getActionIds());
		if (!role.isSystemAdmin() && grantGuard.isLastActiveAdmin(user.getRole(), userId)) {
			throw new BusinessException(ErrorCode.LAST_ADMIN_PROTECTED);
		}
		user.changeRole(role);
		eventPublisher.publishEvent(new AuthzChangedEvent.UserChanged(userId));
		log.info("역할 변경: userId={}, roleId={}, by={}", userId, roleId, actor.userId());
		return UserAdminResponse.from(user);
	}

	public UserAdminResponse unlock(Long userId) {
		User user = findUser(userId);
		user.unlock();
		eventPublisher.publishEvent(new AuthzChangedEvent.UserChanged(userId));
		log.info("계정 잠금 해제: userId={}", userId);
		return UserAdminResponse.from(user);
	}

	private User findUser(Long userId) {
		return userRepository.findWithRoleById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}
}
