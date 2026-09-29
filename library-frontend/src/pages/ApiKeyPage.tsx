import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { errorMessage } from '../api/client';
import { apiKeyApi, authzAdminApi } from '../api/endpoints';
import type { ApiKey, IssuedApiKey, MenuTree } from '../api/types';
import { Can } from '../components/Can';
import { Modal } from '../components/Modal';
import { useNotice } from '../components/Notice';
import { useAction } from '../hooks/useAction';

const splitIps = (text: string) => text.split(/[\n,]/).map((ip) => ip.trim()).filter(Boolean);
const minutes = (value: string | null) => value?.replace('T', ' ').slice(0, 16) ?? '-';

/** 집합에서 id 를 넣거나 뺀 새 집합 */
function toggled(set: Set<number>, id: number): Set<number> {
  const next = new Set(set);
  if (!next.delete(id)) {
    next.add(id);
  }
  return next;
}

/** 메뉴별 액션 체크박스 */
function ActionPicker({ menus, selected, onToggle }: { menus: MenuTree[]; selected: Set<number>; onToggle: (id: number) => void }) {
  return (
    <div className="action-picker">
      {menus.map((menu) => (
        <fieldset key={menu.id}>
          <legend>{menu.name}</legend>
          {menu.actions.map((action) => (
            <label key={action.id} className="inline">
              <input type="checkbox" checked={selected.has(action.id)} onChange={() => onToggle(action.id)} />
              {action.name}
            </label>
          ))}
        </fieldset>
      ))}
    </div>
  );
}

/** 발급 폼: 이름·부여 액션·허용 IP·만료 */
function IssueForm({ menus, onIssued }: { menus: MenuTree[]; onIssued: (issued: IssuedApiKey) => void }) {
  const { notify } = useNotice();
  const [name, setName] = useState('');
  const [actionIds, setActionIds] = useState<Set<number>>(new Set());
  const [ips, setIps] = useState('');
  const [expiresAt, setExpiresAt] = useState('');

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const body = { name: name.trim(), actionIds: [...actionIds], allowedIps: splitIps(ips), expiresAt: expiresAt ? `${expiresAt}:00` : null };
    try {
      onIssued(await apiKeyApi.issue(body));
      setName('');
      setActionIds(new Set());
    } catch (e) {
      notify(errorMessage(e), 'error');
    }
  };

  return (
    <form className="card issue-form" onSubmit={onSubmit}>
      <h2>새 API Key 발급</h2>
      <div className="form-grid">
        <label>이름(용도)<input value={name} onChange={(e) => setName(e.target.value)} required /></label>
        <label>
          만료 시각(비우면 만료 없음)
          <input type="datetime-local" value={expiresAt} onChange={(e) => setExpiresAt(e.target.value)} />
        </label>
        <label className="full-row">
          허용 IP·CIDR (한 줄에 하나, 비우면 전체 허용)
          <textarea rows={2} value={ips} onChange={(e) => setIps(e.target.value)} />
        </label>
      </div>
      <div className="muted">부여할 액션 (본인이 보유한 액션만 부여할 수 있습니다)</div>
      <ActionPicker menus={menus} selected={actionIds} onToggle={(id) => setActionIds(toggled(actionIds, id))} />
      <button className="btn primary" disabled={actionIds.size === 0}>발급</button>
    </form>
  );
}

function ApiKeyTable({ keys, onRevoke }: { keys: ApiKey[]; onRevoke: (key: ApiKey) => void }) {
  return (
    <table className="table">
      <thead>
        <tr>
          <th>이름</th><th>키</th><th>액션</th><th>허용 IP</th><th>만료</th><th>마지막 사용</th><th>상태</th><th />
        </tr>
      </thead>
      <tbody>
        {keys.map((key) => (
          <tr key={key.id}>
            <td>{key.name}</td>
            <td><code>{key.keyPrefix}…</code></td>
            <td>{key.actionCodes.join(', ')}</td>
            <td>{key.allowedIps.length ? key.allowedIps.join(', ') : '전체'}</td>
            <td>{minutes(key.expiresAt)}</td>
            <td>{minutes(key.lastUsedAt)}</td>
            <td><span className={`badge ${key.status === 'ACTIVE' ? 'ok' : 'danger'}`}>{key.status}</span></td>
            <td>
              {key.status === 'ACTIVE' && (
                <Can url="POST /api/admin/api-keys/{id}/revoke">
                  <button className="btn small danger" onClick={() => onRevoke(key)}>폐기</button>
                </Can>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/** 발급 직후 원문을 한 번만 보여주는 모달 */
function IssuedKeyModal({ issued, onClose }: { issued: IssuedApiKey; onClose: () => void }) {
  const { notify } = useNotice();
  return (
    <Modal title="API Key 발급 완료" onClose={onClose}>
      <p className="error-text">이 창을 닫으면 원문을 다시 볼 수 없습니다. 안전한 곳에 보관하세요.</p>
      <div className="secret">
        <code>{issued.apiKey}</code>
        <button className="btn small"
          onClick={() => navigator.clipboard.writeText(issued.apiKey).then(() => notify('복사했습니다.', 'success'))}>
          복사
        </button>
      </div>
    </Modal>
  );
}

/** API Key 관리: 목록·발급(원문 1회 표시)·폐기 */
export default function ApiKeyPage() {
  const { notify } = useNotice();
  const [keys, setKeys] = useState<ApiKey[]>([]);
  const [menus, setMenus] = useState<MenuTree[]>([]);
  const [issued, setIssued] = useState<IssuedApiKey | null>(null);

  const load = useCallback(() => {
    apiKeyApi.list().then(setKeys).catch((e) => notify(errorMessage(e), 'error'));
  }, [notify]);
  const run = useAction(load);

  useEffect(load, [load]);
  useEffect(() => {
    authzAdminApi.menus().then(setMenus).catch(() => setMenus([]));
  }, []);

  const revoke = (key: ApiKey) =>
    window.confirm(`「${key.name}」(${key.keyPrefix}…) 을(를) 폐기할까요? 즉시 사용할 수 없게 됩니다.`) &&
    run(() => apiKeyApi.revoke(key.id), '폐기했습니다.');

  return (
    <section className="stack">
      <div className="card">
        <h1>API Key 관리</h1>
        <p className="muted">외부 시스템은 <code>X-API-KEY</code> 헤더로 호출합니다. 원문은 발급 직후 한 번만 볼 수 있습니다.</p>
        <ApiKeyTable keys={keys} onRevoke={revoke} />
      </div>
      <Can url="POST /api/admin/api-keys">
        <IssueForm menus={menus} onIssued={(result) => { setIssued(result); load(); }} />
      </Can>
      {issued && <IssuedKeyModal issued={issued} onClose={() => setIssued(null)} />}
    </section>
  );
}
