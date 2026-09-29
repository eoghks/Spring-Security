package com.example.library.security;

/**
 * JWT 로 인증된 사용자. 역할은 요청마다 캐시에서 읽은 값이다.
 *
 * @param userId   사용자 ID
 * @param username 아이디
 * @param roleId   역할 ID
 */
public record UserPrincipal(Long userId, String username, Long roleId) implements LibraryPrincipal {
}
