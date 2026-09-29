package com.example.library.security;

/**
 * 인증 주체. 사용자(JWT) 또는 API Key 중 하나다.
 */
public sealed interface LibraryPrincipal permits UserPrincipal, ApiKeyPrincipal {
}
