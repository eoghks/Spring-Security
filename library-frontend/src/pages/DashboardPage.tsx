import { useEffect, useState } from 'react';
import { errorMessage } from '../api/client';
import { dashboardApi } from '../api/endpoints';
import type { DashboardStats } from '../api/types';

const CARDS: { key: keyof DashboardStats; label: string }[] = [
  { key: 'titles', label: '도서 종수' },
  { key: 'totalCopies', label: '보유 권수' },
  { key: 'availableCopies', label: '대출 가능 권수' },
  { key: 'activeLoans', label: '대출 중' },
  { key: 'overdueLoans', label: '연체' },
  { key: 'users', label: '회원 수' },
];

/** 대시보드: 간단 통계 */
export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    dashboardApi
      .stats()
      .then(setStats)
      .catch((e) => setError(errorMessage(e)));
  }, []);

  return (
    <section>
      <h1>대시보드</h1>
      {error && <p className="error-text">{error}</p>}
      <div className="stat-grid">
        {CARDS.map(({ key, label }) => (
          <div key={key} className={`card stat ${key === 'overdueLoans' && stats?.overdueLoans ? 'alert' : ''}`}>
            <div className="muted">{label}</div>
            <div className="stat-value">{stats ? stats[key].toLocaleString() : '-'}</div>
          </div>
        ))}
      </div>
    </section>
  );
}
