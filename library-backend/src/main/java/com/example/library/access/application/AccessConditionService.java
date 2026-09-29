package com.example.library.access.application;

import com.example.library.access.api.AccessConditionRequest;
import com.example.library.access.api.AccessConditionResponse;
import com.example.library.access.domain.AccessConditionSnapshot;
import com.example.library.access.domain.UserAccessCondition;
import com.example.library.access.domain.UserAccessConditionRepository;
import com.example.library.authz.cache.AuthzChangedEvent;
import com.example.library.common.api.PageResponse;
import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import com.example.library.user.domain.User;
import com.example.library.user.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 접속 조건 관리. 저장·삭제 후 해당 사용자 캐시만 evict 해 다음 요청부터 즉시 반영한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AccessConditionService {

	private final UserAccessConditionRepository accessConditionRepository;
	private final UserRepository userRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final Clock clock;

	@Transactional(readOnly = true)
	public PageResponse<AccessConditionResponse> list(String keyword, int page, int size) {
		Page<User> users = userRepository.search(keyword.trim(),
				PageRequest.of(Math.max(page, 0), PageResponse.clampSize(size), Sort.by("id")));
		Map<Long, UserAccessCondition> conditions = accessConditionRepository
				.findAllById(users.map(User::getId).toList()).stream()
				.collect(Collectors.toMap(UserAccessCondition::getUserId, Function.identity()));
		return PageResponse.of(users, user -> toResponse(user, Optional.ofNullable(conditions.get(user.getId()))));
	}

	@Transactional(readOnly = true)
	public AccessConditionResponse get(Long userId) {
		User user = findUser(userId);
		return toResponse(user, accessConditionRepository.findById(userId));
	}

	public AccessConditionResponse save(Long userId, AccessConditionRequest request) {
		User user = findUser(userId);
		UserAccessCondition condition = accessConditionRepository.findById(userId)
				.orElseGet(() -> new UserAccessCondition(userId));
		condition.update(request.toSnapshot(), LocalDateTime.now(clock));
		accessConditionRepository.save(condition);
		eventPublisher.publishEvent(new AuthzChangedEvent.UserChanged(userId));
		return toResponse(user, Optional.of(condition));
	}

	public void delete(Long userId) {
		findUser(userId);
		accessConditionRepository.findById(userId).ifPresent(accessConditionRepository::delete);
		eventPublisher.publishEvent(new AuthzChangedEvent.UserChanged(userId));
	}

	private User findUser(Long userId) {
		return userRepository.findWithRoleById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
	}

	/** 조건이 없으면(미설정) 제한 없음으로 응답한다 */
	private AccessConditionResponse toResponse(User user, Optional<UserAccessCondition> condition) {
		AccessConditionSnapshot snapshot = condition.map(UserAccessCondition::toSnapshot)
				.orElseGet(AccessConditionSnapshot::unrestricted);
		return AccessConditionResponse.of(user, condition.isPresent(), snapshot);
	}
}
