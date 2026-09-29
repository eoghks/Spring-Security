import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { bookApi } from '../api/endpoints';
import type { Book, Page } from '../api/types';
import { Pagination } from '../components/Pagination';

const PAGE_SIZE = 10;

/** 도서 목록: 제목·저자·ISBN 검색 + 분류 필터 + 페이지 */
export default function BookListPage() {
  const [keyword, setKeyword] = useState('');
  const [category, setCategory] = useState('');
  const [query, setQuery] = useState({ keyword: '', category: '', page: 0 });
  const [categories, setCategories] = useState<string[]>([]);
  const [result, setResult] = useState<Page<Book> | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    bookApi.categories().then(setCategories).catch(() => setCategories([]));
  }, []);

  useEffect(() => {
    bookApi
      .search({ ...query, size: PAGE_SIZE })
      .then(setResult)
      .catch((e) => setError(errorMessage(e)));
  }, [query]);

  const onSearch = (event: FormEvent) => {
    event.preventDefault();
    setQuery({ keyword, category, page: 0 });
  };

  return (
    <section className="card">
      <h1>도서 목록</h1>
      <form className="toolbar" onSubmit={onSearch}>
        <input placeholder="제목·저자·ISBN" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
        <select value={category} onChange={(e) => setCategory(e.target.value)}>
          <option value="">전체 분류</option>
          {categories.map((c) => (
            <option key={c}>{c}</option>
          ))}
        </select>
        <button className="btn primary">검색</button>
      </form>
      {error && <p className="error-text">{error}</p>}
      <table className="table">
        <thead>
          <tr>
            <th>제목</th>
            <th>저자</th>
            <th>출판사</th>
            <th>분류</th>
            <th>대출 가능</th>
          </tr>
        </thead>
        <tbody>
          {result?.content.map((book) => (
            <tr key={book.id}>
              <td>
                <Link to={`/books/${book.id}`}>{book.title}</Link>
              </td>
              <td>{book.author}</td>
              <td>{book.publisher}</td>
              <td>{book.category}</td>
              <td>
                <span className={`badge ${book.availableQuantity > 0 ? 'ok' : 'warn'}`}>
                  {book.availableQuantity} / {book.totalQuantity}
                </span>
              </td>
            </tr>
          ))}
          {result?.content.length === 0 && (
            <tr>
              <td colSpan={5} className="muted">
                검색 결과가 없습니다.
              </td>
            </tr>
          )}
        </tbody>
      </table>
      {result && (
        <Pagination
          page={result.page}
          totalPages={result.totalPages}
          onChange={(page) => setQuery({ ...query, page })}
        />
      )}
    </section>
  );
}
