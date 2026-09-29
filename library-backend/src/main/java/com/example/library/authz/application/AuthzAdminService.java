package com.example.library.authz.application;

import com.example.library.authz.api.ActionUrlRequest;
import com.example.library.authz.api.MenuTreeResponse;
import com.example.library.authz.api.MenuTreeResponse.ActionNode;
import com.example.library.authz.api.MenuTreeResponse.UrlNode;
import com.example.library.authz.api.RoleActionsResponse;
import com.example.library.authz.cache.AuthzChangedEvent;
import com.example.library.authz.domain.ActionUrl;
import com.example.library.authz.domain.ActionUrlRepository;
import com.example.library.authz.domain.Menu;
import com.example.library.authz.domain.MenuAction;
import com.example.library.authz.domain.MenuActionRepository;
import com.example.library.authz.domain.MenuRepository;
import com.example.library.authz.domain.Role;
import com.example.library.authz.domain.RoleRepository;
import com.example.library.authz.rule.AuthorizationRuleBroadcaster.AuthorizationRulesChangedEvent;
import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import com.example.library.security.UserPrincipal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 역할·권한 관리. 역할 액션 변경은 역할 캐시 evict, URL 매핑 변경은 규칙 재적재(클러스터 전파)로 이어진다.
 * 권한 상승을 막기 위해 자기 역할은 편집할 수 없고, 새로 부여하는 액션은 행위자가 보유한 액션 이내여야 한다(GrantGuard).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthzAdminService {

	private final MenuRepository menuRepository;
	private final MenuActionRepository menuActionRepository;
	private final ActionUrlRepository actionUrlRepository;
	private final RoleRepository roleRepository;
	private final GrantGuard grantGuard;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional(readOnly = true)
	public List<MenuTreeResponse> menuTree() {
		Map<Long, List<UrlNode>> urlsByAction = actionUrlRepository.findAllWithAction().stream()
				.collect(Collectors.groupingBy(url -> url.getAction().getId(),
						Collectors.mapping(url -> new UrlNode(url.getId(), url.getHttpMethod(), url.getUrlPattern()),
								Collectors.toList())));
		Map<Long, List<ActionNode>> actionsByMenu = menuActionRepository.findAllWithMenu().stream()
				.collect(Collectors.groupingBy(action -> action.getMenu().getId(),
						Collectors.mapping(action -> toNode(action, urlsByAction), Collectors.toList())));
		return menuRepository.findAllByOrderBySortOrderAsc().stream()
				.map(menu -> toTree(menu, actionsByMenu.getOrDefault(menu.getId(), List.of())))
				.toList();
	}

	@Transactional(readOnly = true)
	public RoleActionsResponse roleActions(Long roleId) {
		Role role = findRole(roleId);
		return new RoleActionsResponse(roleId, role.getActionIds().stream().sorted().toList());
	}

	/**
	 * 역할 액션 전체 교체. 관리자(시스템) 역할은 잠금 방지를 위해 편집할 수 없다.
	 * 자기 역할은 편집할 수 없고, 새로 추가하는 액션은 행위자가 보유한 것이어야 한다(회수는 제한하지 않음).
	 */
	public RoleActionsResponse replaceRoleActions(UserPrincipal actor, Long roleId, List<Long> actionIds) {
		Role role = findRole(roleId);
		if (role.isSystemAdmin()) {
			throw new BusinessException(ErrorCode.SYSTEM_ROLE_PROTECTED);
		}
		Role actorRole = grantGuard.actorRole(actor);
		grantGuard.ensureNotOwnRole(actorRole, roleId);
		Set<Long> requested = new HashSet<>(actionIds);
		if (menuActionRepository.findAllById(requested).size() != requested.size()) {
			throw new BusinessException(ErrorCode.INVALID_ACTION);
		}
		Set<Long> added = new HashSet<>(requested);
		added.removeAll(role.getActionIds());
		grantGuard.ensureOwned(actorRole, added);
		role.replaceActions(requested);
		eventPublisher.publishEvent(new AuthzChangedEvent.RoleChanged(roleId));
		log.info("역할 권한 변경: roleId={}, 액션 {}개", roleId, requested.size());
		return new RoleActionsResponse(roleId, requested.stream().sorted().toList());
	}

	/** 액션에 URL 추가. 자기 역할이 보유한 액션에는 URL 을 붙일 수 없다(자기 권한 확장 방지) */
	public UrlNode addActionUrl(UserPrincipal actor, Long actionId, ActionUrlRequest request) {
		MenuAction action = menuActionRepository.findById(actionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ACTION_NOT_FOUND));
		grantGuard.ensureNotOwnAction(grantGuard.actorRole(actor), actionId);
		if (actionUrlRepository.existsByActionIdAndHttpMethodAndUrlPattern(actionId, request.httpMethod(),
				request.urlPattern())) {
			throw new BusinessException(ErrorCode.DUPLICATE_ACTION_URL);
		}
		ActionUrl saved = actionUrlRepository.save(ActionUrl.builder()
				.action(action).httpMethod(request.httpMethod()).urlPattern(request.urlPattern()).build());
		eventPublisher.publishEvent(new AuthorizationRulesChangedEvent());
		log.info("액션 URL 추가: actionId={}, {} {}", actionId, request.httpMethod(), request.urlPattern());
		return new UrlNode(saved.getId(), saved.getHttpMethod(), saved.getUrlPattern());
	}

	/** 액션 URL 삭제. 추가와 같은 이유로 자기 역할이 보유한 액션의 URL 은 지울 수 없다 */
	public void deleteActionUrl(UserPrincipal actor, Long actionUrlId) {
		ActionUrl url = actionUrlRepository.findById(actionUrlId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ACTION_URL_NOT_FOUND));
		grantGuard.ensureNotOwnAction(grantGuard.actorRole(actor), url.getAction().getId());
		actionUrlRepository.delete(url);
		eventPublisher.publishEvent(new AuthorizationRulesChangedEvent());
		log.info("액션 URL 삭제: id={}, {} {}", actionUrlId, url.getHttpMethod(), url.getUrlPattern());
	}

	private Role findRole(Long roleId) {
		return roleRepository.findById(roleId).orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
	}

	private ActionNode toNode(MenuAction action, Map<Long, List<UrlNode>> urlsByAction) {
		return new ActionNode(action.getId(), action.getCode(), action.getName(), action.getActionType().name(),
				action.authorityCode(), urlsByAction.getOrDefault(action.getId(), List.of()));
	}

	private MenuTreeResponse toTree(Menu menu, List<ActionNode> actions) {
		return new MenuTreeResponse(menu.getId(), menu.getCode(), menu.getName(), menu.getPath(), menu.getSortOrder(),
				actions);
	}
}
