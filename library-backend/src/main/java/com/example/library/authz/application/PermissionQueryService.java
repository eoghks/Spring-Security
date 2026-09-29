package com.example.library.authz.application;

import com.example.library.authz.domain.Menu;
import com.example.library.authz.domain.MenuRepository;
import com.example.library.authz.rule.AuthorizationRuleRegistry;
import com.example.library.security.GrantedActionResolver;
import com.example.library.security.LibraryPrincipal;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 사용자의 권한 요약(호출 가능 URL·진입 가능 메뉴)을 만든다.
 * 프론트는 URL 문자열로 버튼을, 메뉴 코드로 사이드바를 제어한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionQueryService {

	private static final String READ_SUFFIX = ":READ";

	private final GrantedActionResolver grantedActionResolver;
	private final AuthorizationRuleRegistry registry;
	private final MenuRepository menuRepository;

	public MyPermissions permissionsOf(LibraryPrincipal principal) {
		Set<String> granted = grantedActionResolver.grantedActions(principal);
		Set<String> urls = registry.current().callableUrls(granted);
		List<String> menus = menuRepository.findAllByOrderBySortOrderAsc().stream()
				.map(Menu::getCode)
				.filter(code -> granted.contains(code + READ_SUFFIX))
				.toList();
		return new MyPermissions(urls, menus);
	}

	/**
	 * 권한 요약.
	 *
	 * @param urls  호출 가능한 "METHOD /pattern" 문자열 집합(등록 패턴 그대로)
	 * @param menus READ 액션을 보유한 메뉴 코드(정렬 순서)
	 */
	public record MyPermissions(Set<String> urls, List<String> menus) {
	}
}
