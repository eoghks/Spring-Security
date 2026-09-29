import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { errorMessage } from '../api/client';
import { loanManagementApi } from '../api/endpoints';
import type { Loan, Page } from '../api/types';
import { Can } from '../components/Can';
import { LoanStatusBadge } from '../components/LoanStatusBadge';
import { useNotice } from '../components/Notice';
import { Pagination } from '../components/Pagination';

/** 사서 대출 처리 폼(회원 아이디 + 도서 ID) */
function CheckoutForm({ onDone }: { onDone: () => void }) {
  const { notify } = useNotice();
  const [username, setUsername] = useState('');
  const [bookId, setBookId] = useState('');

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      const loan = await loanManagementApi.checkout(username.trim(), Number(bookId));
      notify(`${loan.username} 님에게 「${loan.bookTitle}」 대출 처리했습니다.`, 'success');
      setBookId('');
      onDone();
    } catch (e) {
      notify(errorMessage(e), 'error');
    }
  };

  return (
    <form className="toolbar" onSubmit={onSubmit}>
      <strong>대출 처리</strong>
      <input placeholder="회원 아이디" value={username} onChange={(e) => setUsername(e.target.value)} required />
      <input placeholder="도서 ID" type="number" min={1} value={bookId} onChange={(e) => setBookId(e.target.value)} required />
      <button className="btn primary">대출</button>
    </form>
  );
}

/** 대출 관리(사서): 상태·키워드 필터 목록, 대출 처리, 반납 처리 */
export default function LoanManagementPage() {
  const { notify } = useNotice();
  const [status, setStatus] = useState('');
  const [keyword, setKeyword] = useState('');
  const [query, setQuery] = useState({ status: '', keyword: '', page: 0 });
  const [result, setResult] = useState<Page<Loan> | null>(null);

  const load = useCallback(() => {
    loanManagementApi
      .search({ ...query, size: 10 })
      .then(setResult)
      .catch((e) => notify(errorMessage(e), 'error'));
  }, [query, notify]);

  useEffect(load, [load]);

  const processReturn = async (loan: Loan) => {
    try {
      await loanManagementApi.processReturn(loan.id);
      notify('반납 처리했습니다.', 'success');
      load();
    } catch (e) {
      notify(errorMessage(e), 'error');
    }
  };

  const onSearch = (event: FormEvent) => {
    event.preventDefault();
    setQuery({ status, keyword, page: 0 });
  };

  return (
    <section className="card">
      <h1>대출 관리</h1>
      <Can url="POST /api/loan-management">
        <CheckoutForm onDone={load} />
      </Can>
      <form className="toolbar" onSubmit={onSearch}>
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">전체 상태</option>
          <option value="LOANED">대출 중</option>
          <option value="OVERDUE">연체</option>
          <option value="RETURNED">반납</option>
        </select>
        <input placeholder="회원 아이디·도서 제목" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
        <button className="btn">검색</button>
      </form>
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>회원</th>
            <th>도서</th>
            <th>대출일</th>
            <th>반납 예정일</th>
            <th>상태</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {result?.content.map((loan) => (
            <tr key={loan.id}>
              <td>{loan.id}</td>
              <td>{loan.username}</td>
              <td>{loan.bookTitle}</td>
              <td>{loan.loanDate}</td>
              <td>{loan.dueDate}</td>
              <td>
                <LoanStatusBadge status={loan.status} />
              </td>
              <td>
                {loan.status !== 'RETURNED' && (
                  <Can url="POST /api/loan-management/{id}/return">
                    <button className="btn small" onClick={() => processReturn(loan)}>
                      반납 처리
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
    </section>
  );
}
