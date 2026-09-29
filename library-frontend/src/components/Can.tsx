import type { ReactNode } from 'react';
import { usePermissions } from '../auth/PermissionContext';

interface CanProps {
  /**
   * 버튼이 부르는 주 동작 URL — 백엔드 action_urls 에 등록된 패턴 문자열 그대로(예: "POST /api/loans").
   * 오타가 나면 버튼이 조용히 사라지므로 `npm run check:can` 이 시드와 대조한다.
   */
  url: string;
  children: ReactNode;
  /** 권한이 없을 때 대신 보여줄 내용 */
  fallback?: ReactNode;
}

/** 권한이 있을 때만 children 을 그린다 */
export function Can({ url, children, fallback = null }: CanProps) {
  const { can } = usePermissions();
  return <>{can(url) ? children : fallback}</>;
}
