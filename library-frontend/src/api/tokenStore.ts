import type { TokenResponse } from './types';

// 토큰 보관소. 개발 편의상 localStorage 를 쓴다(트레이드오프는 docs/architecture.md 참고 — XSS 에 노출될 수 있음).
const ACCESS_KEY = 'library.accessToken';
const REFRESH_KEY = 'library.refreshToken';

export const tokenStore = {
  accessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  },
  refreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  },
  save(tokens: TokenResponse): void {
    localStorage.setItem(ACCESS_KEY, tokens.accessToken);
    localStorage.setItem(REFRESH_KEY, tokens.refreshToken);
  },
  clear(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};
