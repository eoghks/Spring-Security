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

function refreshAccessToken(): Promise<string> {
  if (!refreshing) {
    const refreshToken = tokenStore.refreshToken();
    refreshing = (refreshToken
      ? axios.post<TokenResponse>('/api/auth/refresh', { refreshToken }).then(({ data }) => {
          tokenStore.save(data);
          return data.accessToken;
        })
      : Promise.reject(new Error('리프레시 토큰 없음'))
    ).finally(() => {
      refreshing = null;
    });
  }
  return refreshing;
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
        const accessToken = await refreshAccessToken();
        config.headers.Authorization = `Bearer ${accessToken}`;
        return api(config as AxiosRequestConfig);
      } catch {
        tokenStore.clear();
        emitAuthExpired();
      }
    } else if (status === 403) {
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

/** 오류 응답의 필드 오류를 { 필드: 메시지 } 로 변환한다 */
export function fieldErrors(error: unknown): Record<string, string> {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    return Object.fromEntries((error.response?.data?.fieldErrors ?? []).map((e) => [e.field, e.message]));
  }
  return {};
}
