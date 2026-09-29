import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { bookApi, loanApi } from '../api/endpoints';
import type { Book } from '../api/types';
import { Can } from '../components/Can';
import { useNotice } from '../components/Notice';

function BookInfo({ book }: { book: Book }) {
  return (
    <dl className="detail">
      <dt>저자</dt>
      <dd>{book.author}</dd>
      <dt>출판사</dt>
      <dd>{book.publisher}</dd>
      <dt>분류</dt>
      <dd>{book.category}</dd>
      <dt>ISBN</dt>
      <dd>{book.isbn}</dd>
      <dt>대출 가능</dt>
      <dd>
        {book.availableQuantity} / {book.totalQuantity}
      </dd>
    </dl>
  );
}

/** 도서 상세 + 대출 신청(BOOK:BORROW 보유 시에만 버튼 표시) */
export default function BookDetailPage() {
  const { id } = useParams();
  const { notify } = useNotice();
  const [book, setBook] = useState<Book | null>(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    bookApi
      .get(Number(id))
      .then(setBook)
      .catch((e) => setError(errorMessage(e)));
  }, [id]);

  useEffect(load, [load]);

  const borrow = (target: Book) =>
    loanApi
      .borrow(target.id)
      .then((loan) => {
        notify(`대출되었습니다. 반납 예정일: ${loan.dueDate}`, 'success');
        load();
      })
      .catch((e) => notify(errorMessage(e), 'error'));

  if (error || !book) {
    return error ? <p className="error-text">{error}</p> : <p className="muted">불러오는 중…</p>;
  }
  return (
    <section className="card">
      <Link to="/books">← 목록</Link>
      <h1>{book.title}</h1>
      <BookInfo book={book} />
      <Can url="POST /api/loans">
        <button className="btn primary" disabled={book.availableQuantity === 0} onClick={() => borrow(book)}>
          {book.availableQuantity === 0 ? '대출 불가(재고 없음)' : '대출 신청'}
        </button>
      </Can>
    </section>
  );
}
