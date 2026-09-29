import { useCallback, useEffect, useState } from 'react';
import { errorMessage } from '../api/client';
import { authzAdminApi, userAdminApi } from '../api/endpoints';
import type { AdminUser, Page, Role } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { Can } from '../components/Can';
import { useNotice } from '../components/Notice';
import { Pagination } from '../components/Pagination';
import { SearchBar } from '../components/SearchBar';
import { useAction } from '../hooks/useAction';

interface RowProps {
  user: AdminUser;
  roles: Role[];
  isSelf: boolean;
  run: (task: () => Promise<unknown>, success: string) => Promise<boolean>;
}

/** 회원 한 줄: 역할 선택(CHANGE_ROLE), 잠금 해제(UNLOCK) */
function UserRow({ user, roles, isSelf, run }: RowProps) {
  return (
    <tr>
      <td>{user.username}</td>
      <td>{user.name}</td>
      <td>{user.email}</td>
      <td>
        <Can url="PUT /api/admin/users/{id}/role" fallback={user.roleName}>
          <select
            value={user.roleId}
            disabled={isSelf}
            title={isSelf ? '자기 역할은 바꿀 수 없습니다' : ''}
            onChange={(e) => run(() => userAdminApi.changeRole(user.id, Number(e.target.value)), '역할을 변경했습니다.')}
          >
            {roles.map((role) => (
              <option key={role.id} value={role.id}>{role.name}</option>
            ))}
          </select>
        </Can>
      </td>
      <td>
        <span className={`badge ${user.locked ? 'danger' : 'ok'}`}>{user.locked ? '잠김' : '정상'}</span>
        {user.failedLoginCount > 0 && <span className="muted"> 실패 {user.failedLoginCount}회</span>}
      </td>
      <td>
        {user.locked && (
          <Can url="POST /api/admin/users/{id}/unlock">
            <button className="btn small" onClick={() => run(() => userAdminApi.unlock(user.id), '잠금을 해제했습니다.')}>
              잠금 해제
            </button>
          </Can>
        )}
      </td>
    </tr>
  );
}

/** 회원 관리: 목록 + 역할 변경 + 잠금 해제 */
export default function UserManagementPage() {
  const { me } = useAuth();
  const { notify } = useNotice();
  const [query, setQuery] = useState({ keyword: '', page: 0 });
  const [result, setResult] = useState<Page<AdminUser> | null>(null);
  const [roles, setRoles] = useState<Role[]>([]);

  const load = useCallback(() => {
    userAdminApi.search({ ...query, size: 10 }).then(setResult).catch((e) => notify(errorMessage(e), 'error'));
  }, [query, notify]);
  const run = useAction(load);

  useEffect(load, [load]);
  useEffect(() => {
    authzAdminApi.roles().then(setRoles).catch(() => setRoles([]));
  }, []);

  return (
    <section className="card">
      <h1>회원 관리</h1>
      <SearchBar placeholder="아이디·이름" onSearch={(keyword) => setQuery({ keyword, page: 0 })} />
      <table className="table">
        <thead>
          <tr>
            <th>아이디</th>
            <th>이름</th>
            <th>이메일</th>
            <th>역할</th>
            <th>상태</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {result?.content.map((user) => (
            <UserRow key={user.id} user={user} roles={roles} isSelf={user.id === me?.id} run={run} />
          ))}
        </tbody>
      </table>
      {result && (
        <Pagination page={result.page} totalPages={result.totalPages} onChange={(page) => setQuery({ ...query, page })} />
      )}
    </section>
  );
}
