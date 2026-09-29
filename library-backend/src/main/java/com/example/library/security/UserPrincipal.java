package com.example.library.security;

import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;

/**
 * JWT 로 인증된 사용자. 역할은 요청마다 캐시에서 읽은 값이다.
 *
 * @param userId   사용자 ID
 * @param username 아이디
 * @param roleId   역할 ID
 */
public record UserPrincipal(Long userId, String username, Long roleId) implements LibraryPrincipal {

	/**
	 * 사용자 본인이 필요한 기능(내 대출, API Key 발급, 역할 변경 등)에서 호출자를 사용자로 한정한다.
	 * API Key 로 들어온 요청은 URL 인가를 통과했더라도 403 USER_ONLY 로 거절한다(주체 불일치로 500 이 나지 않게).
	 */
	public static UserPrincipal require(LibraryPrincipal principal) {
		if (principal instanceof UserPrincipal user) {
			return user;
		}
		throw new BusinessException(ErrorCode.USER_ONLY);
	}
}
