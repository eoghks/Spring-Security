import axios, { AxiosError, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios';
import { emitAuthExpired, emitPermissionDenied } from './events';
import { tokenStore } from './tokenStore';
import type { ErrorResponse, TokenResponse } from './types';

/** 재시도 여부 표시가 붙은 요청 설정 */
interface RetriableConfig extends InternalAxiosRequestConfig {
  _retried?: boolean;
}

const AUTH_PATH = '/api/auth/';

export const api = axios.create({ baseURL: '/', headers: { 'Content-Type': 'application/json' } });

// 요청: Access 토큰을 붙인다(인증 API 제외)
api.interceptors.request.use((config) => {
  const token = tokenStore.accessToken();
  if (token && !config.url?.startsWith(AUTH_PATH)) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * 동시에 여러 요청이 401 을 받아도 재발급은 한 번만 하도록 진행 중인 Promise 를 공유한다.
 * 재발급 호출은 인터셉터가 없는 기본 axios 로 보내 무한 루프를 막는다.
 */
let refreshing: Promise<string> | null = null;

/** 탭 간 재발급 직렬화에 쓰는 Web Locks 이름 */
const REFRESH_LOCK = 'library.refresh';

/**
 * Access 토큰을 재발급한다.
 * 탭 안에서는 진행 중인 Promise 를 공유하고, 탭 사이에서는 Web Locks 로 한 번에 한 탭만 재발급한다.
 * 같은 Refresh 토큰을 두 탭이 동시에 내면 서버가 재사용으로 보고 전체 로그아웃하므로, 락을 얻은 뒤
 * 다른 탭이 이미 새 토큰을 저장했으면(실패한 요청의 토큰과 다르면) 서버를 부르지 않고 그 토큰을 쓴다.
 */
function refreshAccessToken(staleAccessToken: string | undefined): Promise<string> {
  if (!refreshing) {
    refreshing = withCrossTabLock(() => refreshOnce(staleAccessToken)).finally(() => {
      refreshing = null;
    });
  }
  return refreshing;
}

async function refreshOnce(staleAccessToken: string | undefined): Promise<string> {
  const current = tokenStore.accessToken();
  if (current && current !== staleAccessToken) {
    return current;
  }
  const refreshToken = tokenStore.refreshToken();
  if (!refreshToken) {
    throw new Error('리프레시 토큰 없음');
  }
  const { data } = await axios.post<TokenResponse>('/api/auth/refresh', { refreshToken });
  tokenStore.save(data);
  return data.accessToken;
}

/** Web Locks 를 지원하면 탭 간 배타 락 안에서 실행하고, 지원하지 않으면 그냥 실행한다 */
async function withCrossTabLock<T>(task: () => Promise<T>): Promise<T> {
  if (typeof navigator !== 'undefined' && navigator.locks) {
    // 락은 task 가 끝날(Promise 가 정리될) 때까지 유지된다
    return await navigator.locks.request(REFRESH_LOCK, task);
  }
  return task();
}

/** 실패한 요청에 실려 간 Access 토큰(재발급 전 값) */
function sentAccessToken(config: RetriableConfig): string | undefined {
  const header = config.headers.Authorization;
  return typeof header === 'string' && header.startsWith('Bearer ') ? header.slice('Bearer '.length) : undefined;
}

// 응답: 401 → 재발급 후 1회 재시도, 403 → 권한 재조회 알림
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ErrorResponse>) => {
    const config = error.config as RetriableConfig | undefined;
    const status = error.response?.status;
    if (status === 401 && config && !config._retried && !config.url?.startsWith(AUTH_PATH)) {
      config._retried = true;
      try {
        const accessToken = await refreshAccessToken(sentAccessToken(config));
        config.headers.Authorization = `Bearer ${accessToken}`;
        return api(config as AxiosRequestConfig);
      } catch {
        tokenStore.clear();
        emitAuthExpired();
      }
    } else if (status === 403 && !config?.url?.startsWith(AUTH_PATH)) {
      // 인증 API(로그인 등)의 403 은 화면이 직접 안내하므로 권한 재조회 알림을 보내지 않는다
      const body = error.response?.data;
      emitPermissionDenied({ code: body?.code ?? 'ACCESS_DENIED', message: body?.message ?? '접근 권한이 없습니다.' });
    }
    return Promise.reject(error);
  },
);

/** 오류 응답에서 사용자 표시용 메시지를 꺼낸다 */
export function errorMessage(error: unknown, fallback = '요청을 처리하지 못했습니다.'): string {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    return error.response?.data?.message ?? fallback;
  }
  return fallback;
}

/** 오류 응답의 code 를 꺼낸다(없으면 빈 문자열) */
export function errorCode(error: unknown): string {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    return error.response?.data?.code ?? '';
  }
  return '';
}

/** 오류 응답의 필드 오류를 { 필드: 메시지 } 로 변환한다 */
export function fieldErrors(error: unknown): Record<string, string> {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    return Object.fromEntries((error.response?.data?.fieldErrors ?? []).map((e) => [e.field, e.message]));
  }
  return {};
}
