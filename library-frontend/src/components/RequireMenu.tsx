import type { ReactNode } from 'react';
import { usePermissions } from '../auth/PermissionContext';

/** 메뉴 READ 권한(하나라도)이 있을 때만 화면을 보여준다. 최종 차단은 서버가 한다 */
export function RequireMenu({ codes, children }: { codes: string[]; children: ReactNode }) {
  const { hasMenu } = usePermissions();
  if (!codes.some(hasMenu)) {
    return (
      <div className="card">
        <h2>접근 권한이 없습니다</h2>
        <p className="muted">이 화면을 볼 수 있는 권한이 없습니다. 관리자에게 문의하세요.</p>
      </div>
    );
  }
  return <>{children}</>;
}
