import { useState, type ReactNode } from 'react';

interface SearchBarProps {
  placeholder: string;
  onSearch: (keyword: string) => void;
  /** 키워드 입력 옆에 둘 추가 필터(선택) */
  children?: ReactNode;
}

/** 키워드 검색 폼(엔터 또는 검색 버튼) */
export function SearchBar({ placeholder, onSearch, children }: SearchBarProps) {
  const [keyword, setKeyword] = useState('');
  return (
    <form
      className="toolbar"
      onSubmit={(e) => {
        e.preventDefault();
        onSearch(keyword.trim());
      }}
    >
      <input placeholder={placeholder} value={keyword} onChange={(e) => setKeyword(e.target.value)} />
      {children}
      <button className="btn">검색</button>
    </form>
  );
}
