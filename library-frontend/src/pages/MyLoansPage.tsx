import { useCallback, useEffect, useState } from 'react';
import { errorMessage } from '../api/client';
import { loanApi } from '../api/endpoints';
import type { Loan } from '../api/types';
import { Can } from '../components/Can';
import { LoanStatusBadge } from '../components/LoanStatusBadge';
import { useNotice } from '../components/Notice';

/** 내 대출 목록 + 반납(MY_LOAN:RETURN) */
export default function MyLoansPage() {
  const { notify } = useNotice();
  const [loans, setLoans] = useState<Loan[]>([]);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    loanApi
      .myLoans()
      .then(setLoans)
      .catch((e) => setError(errorMessage(e)));
  }, []);

  useEffect(load, [load]);

  const returnBook = async (loan: Loan) => {
    try {
      await loanApi.returnMine(loan.id);
      notify(`「${loan.bookTitle}」을(를) 반납했습니다.`, 'success');
      load();
    } catch (e) {
      notify(errorMessage(e), 'error');
    }
  };

  const active = loans.filter((loan) => loan.status !== 'RETURNED').length;
  return (
    <section className="card">
      <h1>내 대출</h1>
      <p className="muted">대출 중 {active}권 / 최대 5권 · 연체 중이면 새로 대출할 수 없습니다.</p>
      {error && <p className="error-text">{error}</p>}
      <table className="table">
        <thead>
          <tr>
            <th>도서</th>
            <th>대출일</th>
            <th>반납 예정일</th>
            <th>반납일</th>
            <th>상태</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {loans.map((loan) => (
            <tr key={loan.id}>
              <td>{loan.bookTitle}</td>
              <td>{loan.loanDate}</td>
              <td>{loan.dueDate}</td>
              <td>{loan.returnedDate ?? '-'}</td>
              <td>
                <LoanStatusBadge status={loan.status} />
              </td>
              <td>
                {loan.status !== 'RETURNED' && (
                  <Can url="POST /api/loans/{id}/return">
                    <button className="btn small" onClick={() => returnBook(loan)}>
                      반납
                    </button>
                  </Can>
                )}
              </td>
            </tr>
          ))}
          {loans.length === 0 && (
            <tr>
              <td colSpan={6} className="muted">
                대출 내역이 없습니다.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </section>
  );
}
