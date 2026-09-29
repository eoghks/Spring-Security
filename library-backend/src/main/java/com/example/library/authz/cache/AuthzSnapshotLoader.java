package com.example.library.authz.cache;

import com.example.library.authz.domain.MenuAction;
import com.example.library.authz.domain.MenuActionRepository;
import com.example.library.authz.domain.RoleRepository;
import com.example.library.user.domain.User;
import com.example.library.user.domain.UserRepository;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 캐시 미스 시 DB 에서 스냅샷을 읽는다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthzSnapshotLoader {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final MenuActionRepository menuActionRepository;

	public Optional<UserAuthSnapshot> loadUser(Long userId) {
		return userRepository.findWithRoleById(userId).map(this::toSnapshot);
	}

	public RoleActionsSnapshot loadRoleActions(Long roleId) {
		Set<Long> actionIds = roleRepository.findById(roleId)
				.map(role -> Set.copyOf(role.getActionIds()))
				.orElseGet(Set::of);
		return new RoleActionsSnapshot(roleId, toActionCodes(actionIds));
	}

	/** 액션 ID 목록을 "메뉴코드:액션코드" 집합으로 바꾼다 */
	public Set<String> toActionCodes(Collection<Long> actionIds) {
		if (actionIds.isEmpty()) {
			return Set.of();
		}
		return menuActionRepository.findAllWithMenuByIdIn(actionIds).stream()
				.map(MenuAction::authorityCode)
				.collect(Collectors.toUnmodifiableSet());
	}

	private UserAuthSnapshot toSnapshot(User user) {
		return new UserAuthSnapshot(user.getId(), user.getUsername(), user.getRoleId(), user.isLocked());
	}
}
