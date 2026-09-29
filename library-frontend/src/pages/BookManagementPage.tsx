import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { errorMessage, fieldErrors } from '../api/client';
import { bookApi } from '../api/endpoints';
import type { Book, BookRequest, Page } from '../api/types';
import { Can } from '../components/Can';
import { Modal } from '../components/Modal';
import { useNotice } from '../components/Notice';
import { Pagination } from '../components/Pagination';

const EMPTY: BookRequest = { isbn: '', title: '', author: '', publisher: '', category: '', totalQuantity: 1 };

interface EditorProps {
  book: Book | null;
  onClose: () => void;
  onSaved: () => void;
}

/** 도서 등록·수정 폼(book 이 null 이면 등록) */
function BookEditor({ book, onClose, onSaved }: EditorProps) {
  const { notify } = useNotice();
  const [form, setForm] = useState<BookRequest>(book ?? EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    try {
      await (book ? bookApi.update(book.id, form) : bookApi.create(form));
      notify(book ? '수정했습니다.' : '등록했습니다.', 'success');
      onSaved();
    } catch (e) {
      setErrors(fieldErrors(e));
      notify(errorMessage(e), 'error');
    }
  };

  const text = (name: Exclude<keyof BookRequest, 'totalQuantity'>, label: string) => (
    <label>
      {label}
      <input value={form[name]} onChange={(e) => setForm({ ...form, [name]: e.target.value })} required />
      {errors[name] && <span className="error-text">{errors[name]}</span>}
    </label>
  );

  return (
    <Modal title={book ? '도서 수정' : '도서 등록'} onClose={onClose}>
      <form className="form-grid" onSubmit={onSubmit}>
        {text('isbn', 'ISBN (10/13자리)')}
        {text('title', '제목')}
        {text('author', '저자')}
        {text('publisher', '출판사')}
        {text('category', '분류')}
        <label>
          보유 수량
          <input
            type="number"
            min={0}
            value={form.totalQuantity}
            onChange={(e) => setForm({ ...form, totalQuantity: Number(e.target.value) })}
          />
          {errors.totalQuantity && <span className="error-text">{errors.totalQuantity}</span>}
        </label>
        <button className="btn primary">저장</button>
      </form>
    </Modal>
  );
}

/** 도서 관리: 목록 + 등록(BOOK_MANAGE:CREATE) · 수정(UPDATE) · 삭제(DELETE) */
export default function BookManagementPage() {
  const { notify } = useNotice();
  const [keyword, setKeyword] = useState('');
  const [query, setQuery] = useState({ keyword: '', page: 0 });
  const [result, setResult] = useState<Page<Book> | null>(null);
  const [editing, setEditing] = useState<Book | 'new' | null>(null);

  const load = useCallback(() => {
    bookApi
      .search({ ...query, size: 10 })
      .then(setResult)
      .catch((e) => notify(errorMessage(e), 'error'));
  }, [query, notify]);

  useEffect(load, [load]);

  const remove = async (book: Book) => {
    if (!window.confirm(`「${book.title}」을(를) 삭제할까요?`)) {
      return;
    }
    try {
      await bookApi.remove(book.id);
      notify('삭제했습니다.', 'success');
      load();
    } catch (e) {
      notify(errorMessage(e), 'error');
    }
  };

  return (
    <section className="card">
      <h1>도서 관리</h1>
      <div className="toolbar">
        <form
          className="toolbar inline"
          onSubmit={(e) => {
            e.preventDefault();
            setQuery({ keyword, page: 0 });
          }}
        >
          <input placeholder="제목·저자·ISBN" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
          <button className="btn">검색</button>
        </form>
        <Can url="POST /api/books">
          <button className="btn primary" onClick={() => setEditing('new')}>
            도서 등록
          </button>
        </Can>
      </div>
      <table className="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>ISBN</th>
            <th>제목</th>
            <th>저자</th>
            <th>분류</th>
            <th>재고</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {result?.content.map((book) => (
            <tr key={book.id}>
              <td>{book.id}</td>
              <td>{book.isbn}</td>
              <td>{book.title}</td>
              <td>{book.author}</td>
              <td>{book.category}</td>
              <td>
                {book.availableQuantity} / {book.totalQuantity}
              </td>
              <td className="actions">
                <Can url="PUT /api/books/{id}">
                  <button className="btn small" onClick={() => setEditing(book)}>
                    수정
                  </button>
                </Can>
                <Can url="DELETE /api/books/{id}">
                  <button className="btn small danger" onClick={() => remove(book)}>
                    삭제
                  </button>
                </Can>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {result && (
        <Pagination page={result.page} totalPages={result.totalPages} onChange={(page) => setQuery({ ...query, page })} />
      )}
      {editing && (
        <BookEditor
          book={editing === 'new' ? null : editing}
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
