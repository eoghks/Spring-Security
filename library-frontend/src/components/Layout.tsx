import { NavLink, Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { usePermissions } from '../auth/PermissionContext';
import { MENUS } from '../menus';

/** 로그인 후 공통 레이아웃: 사이드바(READ 보유 메뉴만) + 본문 */
export function Layout() {
  const { me, loading, logout } = useAuth();
  const { hasMenu, loaded } = usePermissions();

  if (loading) {
    return <p className="page-loading">불러오는 중…</p>;
  }
  if (!me) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="brand">📚 도서관</div>
        <nav>
          {MENUS.filter((menu) => hasMenu(menu.code)).map((menu) => (
            <NavLink key={menu.code} to={menu.path} className={({ isActive }) => (isActive ? 'active' : '')}>
              {menu.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div>
            <strong>{me.name}</strong>
            <div className="muted">
              {me.username} · {me.roleName}
            </div>
          </div>
          <button className="btn small" onClick={logout}>
            로그아웃
          </button>
        </div>
      </aside>
      <main className="content">{loaded ? <Outlet /> : <p className="muted">권한 확인 중…</p>}</main>
    </div>
  );
}

/** 첫 화면: 진입 가능한 첫 메뉴로 보낸다 */
export function HomeRedirect() {
  const { menus, loaded } = usePermissions();
  if (!loaded) {
    return null;
  }
  const first = MENUS.find((menu) => menus.includes(menu.code));
  return first ? <Navigate to={first.path} replace /> : <p className="muted">접근 가능한 메뉴가 없습니다.</p>;
}
