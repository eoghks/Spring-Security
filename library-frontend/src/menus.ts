// 사이드바 메뉴 정의. 표시 여부는 /api/me/permissions 의 menus(READ 보유 메뉴 코드)로 결정한다.

export interface MenuItem {
  code: string;
  label: string;
  path: string;
}

export const MENUS: MenuItem[] = [
  { code: 'DASHBOARD', label: '대시보드', path: '/dashboard' },
  { code: 'BOOK', label: '도서 목록', path: '/books' },
  { code: 'MY_LOAN', label: '내 대출', path: '/my-loans' },
  { code: 'LOAN_MANAGE', label: '대출 관리', path: '/loan-management' },
  { code: 'BOOK_MANAGE', label: '도서 관리', path: '/book-management' },
  { code: 'USER_MANAGE', label: '회원 관리', path: '/user-management' },
  { code: 'ROLE_MANAGE', label: '역할·권한 관리', path: '/role-management' },
  { code: 'ACCESS_CONDITION', label: '접속 조건 관리', path: '/access-conditions' },
  { code: 'API_KEY', label: 'API Key 관리', path: '/api-keys' },
];
