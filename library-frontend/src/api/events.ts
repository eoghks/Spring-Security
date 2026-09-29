// API 계층 → 화면 계층 알림(전역 이벤트). axios 인터셉터는 React 밖에 있으므로 window 이벤트로 전달한다.

export const AUTH_EXPIRED_EVENT = 'library:auth-expired';
export const PERMISSION_DENIED_EVENT = 'library:permission-denied';

export interface PermissionDeniedDetail {
  code: string;
  message: string;
}

export function emitAuthExpired(): void {
  window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT));
}

export function emitPermissionDenied(detail: PermissionDeniedDetail): void {
  window.dispatchEvent(new CustomEvent<PermissionDeniedDetail>(PERMISSION_DENIED_EVENT, { detail }));
}
