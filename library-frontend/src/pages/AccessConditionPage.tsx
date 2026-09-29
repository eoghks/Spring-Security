import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { errorMessage, fieldErrors } from '../api/client';
import { accessConditionApi } from '../api/endpoints';
import type { AccessCondition, AccessConditionRequest, Page } from '../api/types';
import { Can } from '../components/Can';
import { Modal } from '../components/Modal';
import { useNotice } from '../components/Notice';
import { Pagination } from '../components/Pagination';
import { SearchBar } from '../components/SearchBar';
import { useAction } from '../hooks/useAction';

const DAYS = [
  { code: 'MON', label: '월' },
  { code: 'TUE', label: '화' },
  { code: 'WED', label: '수' },
  { code: 'THU', label: '목' },
  { code: 'FRI', label: '금' },
  { code: 'SAT', label: '토' },
  { code: 'SUN', label: '일' },
];

type ConditionForm = Omit<AccessConditionRequest, 'allowedIps'>;

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

const optional = (value: string) => (value === '' ? null : value);

/** 기간·시간·요일 입력 */
function ConditionFields({ form, onChange }: { form: ConditionForm; onChange: (form: ConditionForm) => void }) {
  const input = (key: 'validFrom' | 'validTo' | 'startTime' | 'endTime', label: string, type: string) => (
    <label>
      {label}
      <input type={type} value={form[key] ?? ''} onChange={(e) => onChange({ ...form, [key]: optional(e.target.value) })} />
    </label>
  );
  const toggleDay = (code: string) =>
    onChange({
      ...form,
      allowedDays: form.allowedDays.includes(code) ? form.allowedDays.filter((d) => d !== code) : [...form.allowedDays, code],
    });

  return (
    <>
      {input('validFrom', '시작일', 'date')}
      {input('validTo', '종료일', 'date')}
      {input('startTime', '시작 시각', 'time')}
      {input('endTime', '종료 시각 (그 분까지 포함, 시작보다 이르면 자정을 넘는 구간)', 'time')}
      <div className="full-row">
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
    </>
  );
}

/** 사용자 한 명의 접속 조건 편집 모달 */
function ConditionEditor({ target, onClose, onSaved }: { target: AccessCondition; onClose: () => void; onSaved: () => void }) {
  const { notify } = useNotice();
  const [ips, setIps] = useState(target.allowedIps.join('\n'));
  const [form, setForm] = useState<ConditionForm>({ ...target });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const allowedIps = ips.split(/[\n,]/).map((ip) => ip.trim()).filter(Boolean);
    const { validFrom, validTo, allowedDays, startTime, endTime } = form;
    try {
      await accessConditionApi.save(target.userId, { allowedIps, validFrom, validTo, allowedDays, startTime, endTime });
      notify('접속 조건을 저장했습니다. 다음 요청부터 즉시 적용됩니다.', 'success');
      onSaved();
    } catch (e) {
      setErrors(fieldErrors(e));
      notify(errorMessage(e), 'error');
    }
  };

  return (
    <Modal title={`접속 조건 — ${target.name} (${target.username})`} onClose={onClose}>
      <form className="form-grid" onSubmit={onSubmit}>
        <label className="full-row">
          허용 IP·CIDR (한 줄에 하나, 비우면 전체 허용)
          <textarea rows={3} value={ips} onChange={(e) => setIps(e.target.value)} placeholder={'192.168.0.0/24\n10.0.0.5'} />
        </label>
        <ConditionFields form={form} onChange={setForm} />
        {Object.keys(errors).length > 0 && (
          <ul className="error-text full-row">
            {Object.entries(errors).map(([field, message]) => (
              <li key={field}>{message}</li>
            ))}
          </ul>
        )}
        <button className="btn primary">저장</button>
      </form>
    </Modal>
  );
}

function ConditionTable(props: { rows: AccessCondition[]; onEdit: (c: AccessCondition) => void; onRemove: (c: AccessCondition) => void }) {
  return (
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
        {props.rows.map((c) => (
          <tr key={c.userId}>
            <td>
              {c.name} <span className="muted">({c.username})</span>
            </td>
            <td>{c.roleCode}</td>
            <td className={c.configured ? '' : 'muted'}>{summary(c)}</td>
            <td className="actions">
              <Can url="PUT /api/admin/access-conditions/{userId}">
                <button className="btn small" onClick={() => props.onEdit(c)}>설정</button>
              </Can>
              {c.configured && (
                <Can url="DELETE /api/admin/access-conditions/{userId}">
                  <button className="btn small danger" onClick={() => props.onRemove(c)}>해제</button>
                </Can>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/** 접속 조건 관리: 사용자별 IP / 기간 / 요일 / 시간 */
export default function AccessConditionPage() {
  const { notify } = useNotice();
  const [query, setQuery] = useState({ keyword: '', page: 0 });
  const [result, setResult] = useState<Page<AccessCondition> | null>(null);
  const [editing, setEditing] = useState<AccessCondition | null>(null);

  const load = useCallback(() => {
    accessConditionApi.list({ ...query, size: 10 }).then(setResult).catch((e) => notify(errorMessage(e), 'error'));
  }, [query, notify]);
  const run = useAction(load);

  useEffect(load, [load]);

  return (
    <section className="card">
      <h1>접속 조건 관리</h1>
      <SearchBar placeholder="아이디·이름" onSearch={(keyword) => setQuery({ keyword, page: 0 })} />
      <ConditionTable
        rows={result?.content ?? []}
        onEdit={setEditing}
        onRemove={(c) => run(() => accessConditionApi.remove(c.userId), '접속 조건을 해제했습니다.')}
      />
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
