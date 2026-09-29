import type { LoanStatus } from '../api/types';

const LABELS: Record<LoanStatus, { text: string; tone: string }> = {
  LOANED: { text: '대출 중', tone: 'ok' },
  RETURNED: { text: '반납', tone: '' },
  OVERDUE: { text: '연체', tone: 'danger' },
};

/** 대출 상태 표시 */
export function LoanStatusBadge({ status }: { status: LoanStatus }) {
  const { text, tone } = LABELS[status];
  return <span className={`badge ${tone}`}>{text}</span>;
}
