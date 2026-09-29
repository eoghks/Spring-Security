import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { errorMessage, fieldErrors } from '../api/client';
import { bookApi } from '../api/endpoints';
import type { Book, BookRequest, Page } from '../api/types';
import { Can } from '../components/Can';
import { Modal } from '../components/Modal';
import { useNotice } from '../components/Notice';
import { Pagination } from '../components/Pagination';
import { SearchBar } from '../components/SearchBar';
import { useAction } from '../hooks/useAction';

const EMPTY: BookRequest = { isbn: '', title: '', author: '', publisher: '', category: '', totalQuantity: 1 };
const TEXT_FIELDS: { name: Exclude<keyof BookRequest, 'totalQuantity'>; label: string }[] = [
  { name: 'isbn', label: 'ISBN (10/13자리)' },
  { name: 'title', label: '제목' },
  { name: 'author', label: '저자' },
  { name: 'publisher', label: '출판사' },
  { name: 'category', label: '분류' },
];

/** 도서 등록·수정 폼(book 이 null 이면 등록) */
function BookEditor({ book, onClose, onSaved }: { book: Book | null; onClose: () => void; onSaved: () => void }) {
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

  return (
    <Modal title={book ? '도서 수정' : '도서 등록'} onClose={onClose}>
      <form className="form-grid" onSubmit={onSubmit}>
        {TEXT_FIELDS.map(({ name, label }) => (
          <label key={name}>
            {label}
            <input value={form[name]} onChange={(e) => setForm({ ...form, [name]: e.target.value })} required />
            {errors[name] && <span className="error-text">{errors[name]}</span>}
          </label>
        ))}
        <label>
          보유 수량
          <input type="number" min={0} value={form.totalQuantity}
            onChange={(e) => setForm({ ...form, totalQuantity: Number(e.target.value) })} />
          {errors.totalQuantity && <span className="error-text">{errors.totalQuantity}</span>}
        </label>
        <button className="btn primary">저장</button>
      </form>
    </Modal>
  );
}

function ManagedBookTable(props: { books: Book[]; onEdit: (book: Book) => void; onDelete: (book: Book) => void }) {
  return (
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
        {props.books.map((book) => (
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
                <button className="btn small" onClick={() => props.onEdit(book)}>수정</button>
              </Can>
              <Can url="DELETE /api/books/{id}">
                <button className="btn small danger" onClick={() => props.onDelete(book)}>삭제</button>
              </Can>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/** 도서 관리: 목록 + 등록(BOOK_MANAGE:CREATE) · 수정(UPDATE) · 삭제(DELETE) */
export default function BookManagementPage() {
  const { notify } = useNotice();
  const [query, setQuery] = useState({ keyword: '', page: 0 });
  const [result, setResult] = useState<Page<Book> | null>(null);
  const [editing, setEditing] = useState<Book | 'new' | null>(null);

  const load = useCallback(() => {
    bookApi.search({ ...query, size: 10 }).then(setResult).catch((e) => notify(errorMessage(e), 'error'));
  }, [query, notify]);
  const run = useAction(load);

  useEffect(load, [load]);

  const remove = (book: Book) =>
    window.confirm(`「${book.title}」을(를) 삭제할까요?`) && run(() => bookApi.remove(book.id), '삭제했습니다.');

  return (
    <section className="card">
      <h1>도서 관리</h1>
      <SearchBar placeholder="제목·저자·ISBN" onSearch={(keyword) => setQuery({ keyword, page: 0 })}>
        <Can url="POST /api/books">
          <button type="button" className="btn primary" onClick={() => setEditing('new')}>도서 등록</button>
        </Can>
      </SearchBar>
      <ManagedBookTable books={result?.content ?? []} onEdit={setEditing} onDelete={remove} />
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
