import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { errorMessage } from '../api/client';
import { authzAdminApi } from '../api/endpoints';
import type { ActionNode, MenuTree, Role } from '../api/types';
import { usePermissions } from '../auth/PermissionContext';
import { Can } from '../components/Can';
import { useNotice } from '../components/Notice';
import { useAction } from '../hooks/useAction';

const METHODS = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'];

/** 액션에 URL 추가 폼 */
function AddUrlForm({ actionId, onChanged }: { actionId: number; onChanged: () => void }) {
  const run = useAction(onChanged);
  const [method, setMethod] = useState('GET');
  const [pattern, setPattern] = useState('');

  const onAdd = async (event: FormEvent) => {
    event.preventDefault();
    if (await run(() => authzAdminApi.addActionUrl(actionId, method, pattern.trim()), 'URL 을 추가했습니다.')) {
      setPattern('');
    }
  };

  return (
    <form className="toolbar inline" onSubmit={onAdd}>
      <select value={method} onChange={(e) => setMethod(e.target.value)}>
        {METHODS.map((m) => (
          <option key={m}>{m}</option>
        ))}
      </select>
      <input placeholder="/api/example/{id}" value={pattern} onChange={(e) => setPattern(e.target.value)} required />
      <button className="btn small primary">URL 추가</button>
    </form>
  );
}

/** 액션 하나의 URL 목록 보기·편집(ROLE_MANAGE:URL_EDIT) */
function ActionUrls({ action, onChanged }: { action: ActionNode; onChanged: () => void }) {
  const run = useAction(onChanged);
  return (
    <div className="url-panel">
      <div className="muted">
        {action.authorityCode} — 이 액션이 허용하는 URL (같은 URL 이 여러 액션에 있으면 하나만 보유해도 통과)
      </div>
      <ul className="url-list">
        {action.urls.map((url) => (
          <li key={url.id}>
            <code>
              {url.httpMethod} {url.urlPattern}
            </code>
            <Can url="DELETE /api/admin/action-urls/{id}">
              <button className="btn small danger"
                onClick={() => run(() => authzAdminApi.deleteActionUrl(url.id), 'URL 을 삭제했습니다.')}>
                삭제
              </button>
            </Can>
          </li>
        ))}
        {action.urls.length === 0 && <li className="muted">등록된 URL 이 없습니다.</li>}
      </ul>
      <Can url="POST /api/admin/actions/{id}/urls">
        <AddUrlForm actionId={action.id} onChanged={onChanged} />
      </Can>
    </div>
  );
}

interface MenuRowProps {
  menu: MenuTree;
  checked: Set<number>;
  editable: boolean;
  onToggle: (id: number) => void;
  onChanged: () => void;
}

/** 메뉴 한 줄: READ 체크 + ACTION 체크들 + URL 펼치기 */
function MenuRow({ menu, checked, editable, onToggle, onChanged }: MenuRowProps) {
  const [openActionId, setOpenActionId] = useState<number | null>(null);
  const open = menu.actions.find((a) => a.id === openActionId);

  const box = (action: ActionNode) => (
    <span key={action.id} className="action-check">
      <label className="inline">
        <input type="checkbox" checked={checked.has(action.id)} disabled={!editable} onChange={() => onToggle(action.id)} />
        {action.name}
      </label>
      <button className="link" onClick={() => setOpenActionId(openActionId === action.id ? null : action.id)}>
        URL {action.urls.length}
      </button>
    </span>
  );

  return (
    <>
      <tr>
        <td>
          <strong>{menu.name}</strong>
          <div className="muted">{menu.code}</div>
        </td>
        <td>{menu.actions.filter((a) => a.actionType === 'READ').map(box)}</td>
        <td className="action-cell">{menu.actions.filter((a) => a.actionType === 'ACTION').map(box)}</td>
      </tr>
      {open && (
        <tr>
          <td colSpan={3}>
            <ActionUrls action={open} onChanged={onChanged} />
          </td>
        </tr>
      )}
    </>
  );
}

/** 역할 목록·메뉴 트리·선택 역할의 체크 상태를 불러온다 */
function useRoleData() {
  const { notify } = useNotice();
  const [roles, setRoles] = useState<Role[]>([]);
  const [roleId, setRoleId] = useState<number | null>(null);
  const [menus, setMenus] = useState<MenuTree[]>([]);
  const [checked, setChecked] = useState<Set<number>>(new Set());

  const loadMenus = useCallback(() => {
    authzAdminApi.menus().then(setMenus).catch((e) => notify(errorMessage(e), 'error'));
  }, [notify]);

  useEffect(() => {
    authzAdminApi.roles().then((list) => {
      setRoles(list);
      setRoleId((current) => current ?? list[0]?.id ?? null);
    });
    loadMenus();
  }, [loadMenus]);

  useEffect(() => {
    if (roleId !== null) {
      authzAdminApi.roleActions(roleId).then((r) => setChecked(new Set(r.actionIds)));
    }
  }, [roleId]);

  return { roles, roleId, setRoleId, menus, loadMenus, checked, setChecked };
}

interface RoleToolbarProps {
  roles: Role[];
  roleId: number | null;
  editable: boolean;
  onSelect: (id: number) => void;
  onSave: () => void;
  onReload: () => void;
}

/** 역할 탭 + 저장 + 규칙 재적재 */
function RoleToolbar({ roles, roleId, editable, onSelect, onSave, onReload }: RoleToolbarProps) {
  return (
    <div className="toolbar">
      <div className="role-tabs">
        {roles.map((r) => (
          <button key={r.id} className={`btn ${r.id === roleId ? 'primary' : ''}`} onClick={() => onSelect(r.id)}>
            {r.name}
          </button>
        ))}
      </div>
      <Can url="PUT /api/admin/roles/{id}/actions">
        <button className="btn primary" disabled={!editable} onClick={onSave}>권한 저장</button>
      </Can>
      <Can url="POST /api/admin/authz/reload">
        <button className="btn" onClick={onReload}>규칙 재적재</button>
      </Can>
    </div>
  );
}

/** 역할·권한 관리: 역할별 메뉴 READ/ACTION 부여 + 액션별 URL 편집 */
export default function RoleManagementPage() {
  const { can, reload: reloadMyPermissions } = usePermissions();
  const { roles, roleId, setRoleId, menus, loadMenus, checked, setChecked } = useRoleData();
  const run = useAction();
  const role = roles.find((r) => r.id === roleId);
  const editable = Boolean(role && !role.system && can('PUT /api/admin/roles/{id}/actions'));

  const toggle = (id: number) => {
    const next = new Set(checked);
    if (!next.delete(id)) {
      next.add(id);
    }
    setChecked(next);
  };

  const save = () =>
    roleId !== null &&
    run(async () => {
      await authzAdminApi.saveRoleActions(roleId, [...checked]);
      await reloadMyPermissions();
    }, '권한을 저장했습니다. 해당 역할 사용자에게 즉시 반영됩니다.');
  const reloadRules = () => run(authzAdminApi.reload, '모든 노드의 인가 규칙을 다시 적재했습니다.').then(loadMenus);

  return (
    <section className="card">
      <h1>역할·권한 관리</h1>
      <RoleToolbar roles={roles} roleId={roleId} editable={editable} onSelect={setRoleId} onSave={save} onReload={reloadRules} />
      {role?.system && <p className="muted">관리자 역할은 모든 액션을 가지며 잠금 방지를 위해 편집할 수 없습니다.</p>}
      <table className="table">
        <thead>
          <tr>
            <th>메뉴</th>
            <th>화면 진입(READ)</th>
            <th>버튼 동작(ACTION)</th>
          </tr>
        </thead>
        <tbody>
          {menus.map((menu) => (
            <MenuRow key={menu.id} menu={menu} checked={checked} editable={editable} onToggle={toggle} onChanged={loadMenus} />
          ))}
        </tbody>
      </table>
    </section>
  );
}
