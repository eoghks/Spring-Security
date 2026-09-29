import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { AUTH_EXPIRED_EVENT } from '../api/events';
import { authApi, meApi } from '../api/endpoints';
import { tokenStore } from '../api/tokenStore';
import type { Me } from '../api/types';

interface AuthContextValue {
  /** 로그인 사용자(미로그인이면 null) */
  me: Me | null;
  /** 최초 사용자 정보 확인 중 */
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/** 로그인 상태 제공자. 토큰이 있으면 기동 시 /api/me 로 사용자를 복원한다 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!tokenStore.accessToken()) {
      setLoading(false);
      return;
    }
    meApi
      .me()
      .then(setMe)
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false));
  }, []);

  // 재발급까지 실패하면(세션 만료) 로그인 화면으로 보낸다
  useEffect(() => {
    const onExpired = () => {
      setMe(null);
      navigate('/login', { replace: true });
    };
    window.addEventListener(AUTH_EXPIRED_EVENT, onExpired);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, onExpired);
  }, [navigate]);

  const login = useCallback(async (username: string, password: string) => {
    tokenStore.save(await authApi.login(username, password));
    setMe(await meApi.me());
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = tokenStore.refreshToken();
    tokenStore.clear();
    setMe(null);
    if (refreshToken) {
      await authApi.logout(refreshToken).catch(() => undefined);
    }
    navigate('/login', { replace: true });
  }, [navigate]);

  const value = useMemo(() => ({ me, loading, login, logout }), [me, loading, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('AuthProvider 안에서만 사용할 수 있습니다.');
  }
  return context;
}
