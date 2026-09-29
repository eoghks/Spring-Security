import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { PERMISSION_DENIED_EVENT, type PermissionDeniedDetail } from '../api/events';
import { meApi } from '../api/endpoints';
import { useNotice } from '../components/Notice';
import { useAuth } from './AuthContext';

interface PermissionContextValue {
  /** 등록 패턴 문자열 그대로의 "METHOD /pattern" 이 호출 가능하면 true (패턴 매칭은 하지 않는다) */
  can: (url: string) => boolean;
  /** READ 액션을 보유한 메뉴인지 */
  hasMenu: (code: string) => boolean;
  menus: string[];
  loaded: boolean;
  reload: () => Promise<void>;
}

const PermissionContext = createContext<PermissionContextValue | null>(null);

/**
 * 로그인 사용자의 권한(/api/me/permissions)을 보관한다.
 * 403 을 받으면 권한이 바뀌었을 수 있으므로 다시 받아오고 안내한다.
 */
export function PermissionProvider({ children }: { children: ReactNode }) {
  const { me } = useAuth();
  const { notify } = useNotice();
  const [urls, setUrls] = useState<Set<string>>(new Set());
  const [menus, setMenus] = useState<string[]>([]);
  const [loaded, setLoaded] = useState(false);

  const reload = useCallback(async () => {
    const permissions = await meApi.permissions();
    setUrls(new Set(permissions.urls));
    setMenus(permissions.menus);
    setLoaded(true);
  }, []);

  useEffect(() => {
    if (me) {
      reload().catch(() => setLoaded(true));
    } else {
      setUrls(new Set());
      setMenus([]);
      setLoaded(false);
    }
  }, [me, reload]);

  useEffect(() => {
    const onDenied = (event: Event) => {
      const { code, message } = (event as CustomEvent<PermissionDeniedDetail>).detail;
      notify(code === 'ACCESS_CONDITION_DENIED' ? '접속 조건(IP·기간·요일·시간)을 벗어났습니다.' : message, 'error');
      reload().catch(() => undefined);
    };
    window.addEventListener(PERMISSION_DENIED_EVENT, onDenied);
    return () => window.removeEventListener(PERMISSION_DENIED_EVENT, onDenied);
  }, [notify, reload]);

  const value = useMemo<PermissionContextValue>(
    () => ({
      can: (url) => urls.has(url),
      hasMenu: (code) => menus.includes(code),
      menus,
      loaded,
      reload,
    }),
    [urls, menus, loaded, reload],
  );
  return <PermissionContext.Provider value={value}>{children}</PermissionContext.Provider>;
}

export function usePermissions(): PermissionContextValue {
  const context = useContext(PermissionContext);
  if (!context) {
    throw new Error('PermissionProvider 안에서만 사용할 수 있습니다.');
  }
  return context;
}
