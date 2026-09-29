interface PaginationProps {
  /** 0부터 시작하는 현재 페이지 */
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}

/** 이전/다음 + 페이지 번호(최대 5개) */
export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null;
  }
  const start = Math.max(0, Math.min(page - 2, totalPages - 5));
  const pages = Array.from({ length: Math.min(5, totalPages) }, (_, i) => start + i);
  return (
    <div className="pagination">
      <button className="btn small" disabled={page === 0} onClick={() => onChange(page - 1)}>
        이전
      </button>
      {pages.map((p) => (
        <button key={p} className={`btn small ${p === page ? 'primary' : ''}`} onClick={() => onChange(p)}>
          {p + 1}
        </button>
      ))}
      <button className="btn small" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        다음
      </button>
    </div>
  );
}
