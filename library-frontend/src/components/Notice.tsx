import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from 'react';

type NoticeKind = 'info' | 'success' | 'error';

interface Notice {
  kind: NoticeKind;
  text: string;
}

interface NoticeContextValue {
  notify: (text: string, kind?: NoticeKind) => void;
}

const NoticeContext = createContext<NoticeContextValue | null>(null);

/** 화면 상단 안내 메시지(몇 초 뒤 자동으로 사라진다) */
export function NoticeProvider({ children }: { children: ReactNode }) {
  const [notice, setNotice] = useState<Notice | null>(null);
  const timer = useRef<number | undefined>(undefined);

  const notify = useCallback((text: string, kind: NoticeKind = 'info') => {
    window.clearTimeout(timer.current);
    setNotice({ kind, text });
    timer.current = window.setTimeout(() => setNotice(null), 4000);
  }, []);

  const value = useMemo(() => ({ notify }), [notify]);
  return (
    <NoticeContext.Provider value={value}>
      {children}
      {notice && (
        <div className={`notice ${notice.kind}`} role="status" onClick={() => setNotice(null)}>
          {notice.text}
        </div>
      )}
    </NoticeContext.Provider>
  );
}

export function useNotice(): NoticeContextValue {
  const context = useContext(NoticeContext);
  if (!context) {
    throw new Error('NoticeProvider 안에서만 사용할 수 있습니다.');
  }
  return context;
}
