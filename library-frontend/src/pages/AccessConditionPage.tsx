import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { errorMessage, fieldErrors } from '../api/client';
import { accessConditionApi } from '../api/endpoints';
import type { AccessCondition, AccessConditionRequest, Page } from '../api/types';
import { Can } from '../components/Can';
import { Modal } from '../components/Modal';
import { useNotice } from '../components/Notice';
import { Pagination } from '../components/Pagination';

const DAYS = [
  { code: 'MON', label: '월' },
  { code: 'TUE', label: '화' },
  { code: 'WED', label: '수' },
  { code: 'THU', label: '목' },
  { code: 'FRI', label: '금' },
  { code: 'SAT', label: '토' },
  { code: 'SUN', label: '일' },
];

/** 조건 요약 문구(비어 있으면 "제한 없음") */
function summary(c: AccessCondition): string {
  const parts = [
    c.allowedIps.length ? `IP ${c.allowedIps.join(', ')}` : '',
    c.validFrom || c.validTo ? `기간 ${c.validFrom ?? '…'} ~ ${c.validTo ?? '…'}` : '',
    c.allowedDays.length ? `요일 ${c.allowedDays.map((d) => DAYS.find((x) => x.code === d)?.label).join('')}` : '',
    c.startTime || c.endTime ? `시간 ${c.startTime ?? '00:00'}~${c.endTime ?? '23:59'}` : '',
  ].filter(Boolean);
  return parts.length ? parts.join(' · ') : '제한 없음';
}

/** 사용자 한 명의 접속 조건 편집 모달 */
function ConditionEditor({ target, onClose, onSaved }: { target: AccessCondition; onClose: () => void; onSaved: () => void }) {
  const { notify } = useNotice();
  const [ips, setIps] = useState(target.allowedIps.join('\n'));
  const [form, setForm] = useState<Omit<AccessConditionRequest, 'allowedIps'>>({
    validFrom: target.validFrom,
    validTo: target.validTo,
    allowedDays: target.allowedDays,
    startTime: target.startTime,
    endTime: target.endTime,
  });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const toggleDay = (code: string) =>
    setForm({
      ...form,
      allowedDays: form.allowedDays.includes(code) ? form.allowedDays.filter((d) => d !== code) : [...form.allowedDays, code],
    });

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const allowedIps = ips.split(/[\n,]/).map((ip) => ip.trim()).filter(Boolean);
    try {
      await accessConditionApi.save(target.userId, { ...form, allowedIps });
      notify('접속 조건을 저장했습니다. 다음 요청부터 즉시 적용됩니다.', 'success');
      onSaved();
    } catch (e) {
      setErrors(fieldErrors(e));
      notify(errorMessage(e), 'error');
    }
  };

  const optional = (value: string) => (value === '' ? null : value);
  const errorList = Object.entries(errors);

  return (
    <Modal title={`접속 조건 — ${target.name} (${target.username})`} onClose={onClose}>
      <form className="form-grid" onSubmit={onSubmit}>
        <label style={{ gridColumn: '1 / -1' }}>
          허용 IP·CIDR (한 줄에 하나, 비우면 전체 허용)
          <textarea rows={3} value={ips} onChange={(e) => setIps(e.target.value)} placeholder={'192.168.0.0/24\n10.0.0.5'} />
        </label>
        <label>
          시작일
          <input type="date" value={form.validFrom ?? ''} onChange={(e) => setForm({ ...form, validFrom: optional(e.target.value) })} />
        </label>
        <label>
          종료일
          <input type="date" value={form.validTo ?? ''} onChange={(e) => setForm({ ...form, validTo: optional(e.target.value) })} />
        </label>
        <label>
          시작 시각
          <input type="time" value={form.startTime ?? ''} onChange={(e) => setForm({ ...form, startTime: optional(e.target.value) })} />
        </label>
        <label>
          종료 시각 (시작보다 이르면 자정을 넘는 구간)
          <input type="time" value={form.endTime ?? ''} onChange={(e) => setForm({ ...form, endTime: optional(e.target.value) })} />
        </label>
        <div style={{ gridColumn: '1 / -1' }}>
          <div className="muted">허용 요일 (선택 없으면 전체)</div>
          <div className="toolbar inline">
            {DAYS.map((day) => (
              <label key={day.code} className="inline">
                <input type="checkbox" checked={form.allowedDays.includes(day.code)} onChange={() => toggleDay(day.code)} />
                {day.label}
              </label>
            ))}
          </div>
        </div>
        {errorList.length > 0 && (
          <ul className="error-text" style={{ gridColumn: '1 / -1' }}>
            {errorList.map(([field, message]) => (
              <li key={field}>{message}</li>
            ))}
          </ul>
        )}
        <button className="btn primary">저장</button>
      </form>
    </Modal>
  );
}

/** 접속 조건 관리: 사용자별 IP / 기간 / 요일 / 시간 */
export default function AccessConditionPage() {
  const { notify } = useNotice();
  const [keyword, setKeyword] = useState('');
  const [query, setQuery] = useState({ keyword: '', page: 0 });
  const [result, setResult] = useState<Page<AccessCondition> | null>(null);
  const [editing, setEditing] = useState<AccessCondition | null>(null);

  const load = useCallback(() => {
    accessConditionApi
      .list({ ...query, size: 10 })
      .then(setResult)
      .catch((e) => notify(errorMessage(e), 'error'));
  }, [query, notify]);

  useEffect(load, [load]);

  const remove = async (target: AccessCondition) => {
    try {
      await accessConditionApi.remove(target.userId);
      notify('접속 조건을 해제했습니다.', 'success');
      load();
    } catch (e) {
      notify(errorMessage(e), 'error');
    }
  };

  return (
    <section className="card">
      <h1>접속 조건 관리</h1>
      <form
        className="toolbar"
        onSubmit={(e) => {
          e.preventDefault();
          setQuery({ keyword, page: 0 });
        }}
      >
        <input placeholder="아이디·이름" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
        <button className="btn">검색</button>
      </form>
      <table className="table">
        <thead>
          <tr>
            <th>사용자</th>
            <th>역할</th>
            <th>조건</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {result?.content.map((c) => (
            <tr key={c.userId}>
              <td>
                {c.name} <span className="muted">({c.username})</span>
              </td>
              <td>{c.roleCode}</td>
              <td className={c.configured ? '' : 'muted'}>{summary(c)}</td>
              <td className="actions">
                <Can url="PUT /api/admin/access-conditions/{userId}">
                  <button className="btn small" onClick={() => setEditing(c)}>
                    설정
                  </button>
                </Can>
                {c.configured && (
                  <Can url="DELETE /api/admin/access-conditions/{userId}">
                    <button className="btn small danger" onClick={() => remove(c)}>
                      해제
                    </button>
                  </Can>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {result && (
        <Pagination page={result.page} totalPages={result.totalPages} onChange={(page) => setQuery({ ...query, page })} />
      )}
      {editing && (
        <ConditionEditor
          target={editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            load();
          }}
        />
      )}
    </section>
  );
}
