-- =====================================================================
-- 시드 데이터 (개발용)
-- ID 를 직접 지정하지 않고 코드로 조회해 참조한다(IDENTITY 시퀀스 충돌 방지).
-- action_urls INSERT 는 한 줄에 하나씩 둔다 — 프론트의 <Can url> 검사 스크립트가 이 파일을 파싱한다.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 역할
-- ---------------------------------------------------------------------
INSERT INTO roles (code, name) VALUES ('ADMIN', '관리자');
INSERT INTO roles (code, name) VALUES ('LIBRARIAN', '사서');
INSERT INTO roles (code, name) VALUES ('MEMBER', '일반 회원');

-- ---------------------------------------------------------------------
-- 메뉴
-- ---------------------------------------------------------------------
INSERT INTO menus (code, name, path, sort_order) VALUES ('DASHBOARD', '대시보드', '/dashboard', 10);
INSERT INTO menus (code, name, path, sort_order) VALUES ('BOOK', '도서 목록', '/books', 20);
INSERT INTO menus (code, name, path, sort_order) VALUES ('MY_LOAN', '내 대출', '/my-loans', 30);
INSERT INTO menus (code, name, path, sort_order) VALUES ('LOAN_MANAGE', '대출 관리', '/loan-management', 40);
INSERT INTO menus (code, name, path, sort_order) VALUES ('BOOK_MANAGE', '도서 관리', '/book-management', 50);
INSERT INTO menus (code, name, path, sort_order) VALUES ('USER_MANAGE', '회원 관리', '/user-management', 60);
INSERT INTO menus (code, name, path, sort_order) VALUES ('ROLE_MANAGE', '역할·권한 관리', '/role-management', 70);
INSERT INTO menus (code, name, path, sort_order) VALUES ('ACCESS_CONDITION', '접속 조건 관리', '/access-conditions', 80);
INSERT INTO menus (code, name, path, sort_order) VALUES ('API_KEY', 'API Key 관리', '/api-keys', 90);

-- ---------------------------------------------------------------------
-- 메뉴 액션
-- ---------------------------------------------------------------------
INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'READ', '대시보드 조회', 'READ' FROM menus WHERE code = 'DASHBOARD';

INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'READ', '도서 조회', 'READ' FROM menus WHERE code = 'BOOK';
INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'BORROW', '대출 신청', 'ACTION' FROM menus WHERE code = 'BOOK';

INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'READ', '내 대출 조회', 'READ' FROM menus WHERE code = 'MY_LOAN';
INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'RETURN', '반납', 'ACTION' FROM menus WHERE code = 'MY_LOAN';

INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'READ', '대출 현황 조회', 'READ' FROM menus WHERE code = 'LOAN_MANAGE';
INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'CHECKOUT', '대출 처리', 'ACTION' FROM menus WHERE code = 'LOAN_MANAGE';
INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'RETURN', '반납 처리', 'ACTION' FROM menus WHERE code = 'LOAN_MANAGE';

INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'READ', '도서 관리 조회', 'READ' FROM menus WHERE code = 'BOOK_MANAGE';
INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'CREATE', '도서 등록', 'ACTION' FROM menus WHERE code = 'BOOK_MANAGE';
INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'UPDATE', '도서 수정', 'ACTION' FROM menus WHERE code = 'BOOK_MANAGE';
INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'DELETE', '도서 삭제', 'ACTION' FROM menus WHERE code = 'BOOK_MANAGE';

INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'READ', '회원 조회', 'READ' FROM menus WHERE code = 'USER_MANAGE';
INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'CHANGE_ROLE', '역할 변경', 'ACTION' FROM menus WHERE code = 'USER_MANAGE';
INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'UNLOCK', '잠금 해제', 'ACTION' FROM menus WHERE code = 'USER_MANAGE';

INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'READ', '역할·권한 조회', 'READ' FROM menus WHERE code = 'ROLE_MANAGE';
INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'GRANT', '역할 권한 저장', 'ACTION' FROM menus WHERE code = 'ROLE_MANAGE';
INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'URL_EDIT', '액션 URL 편집', 'ACTION' FROM menus WHERE code = 'ROLE_MANAGE';

INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'READ', '접속 조건 조회', 'READ' FROM menus WHERE code = 'ACCESS_CONDITION';
INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'SAVE', '접속 조건 저장', 'ACTION' FROM menus WHERE code = 'ACCESS_CONDITION';
INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'DELETE', '접속 조건 삭제', 'ACTION' FROM menus WHERE code = 'ACCESS_CONDITION';

INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'READ', 'API Key 조회', 'READ' FROM menus WHERE code = 'API_KEY';
INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'ISSUE', 'API Key 발급', 'ACTION' FROM menus WHERE code = 'API_KEY';
INSERT INTO menu_actions (menu_id, code, name, action_type) SELECT id, 'REVOKE', 'API Key 폐기', 'ACTION' FROM menus WHERE code = 'API_KEY';

-- ---------------------------------------------------------------------
-- 액션별 URL (같은 URL 이 여러 액션에 걸리면 그중 하나만 보유해도 통과 — OR)
-- ---------------------------------------------------------------------
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/dashboard/stats' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'DASHBOARD' AND a.code = 'READ';

INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/books' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'BOOK' AND a.code = 'READ';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/books/{id}' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'BOOK' AND a.code = 'READ';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/books/categories' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'BOOK' AND a.code = 'READ';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'POST', '/api/loans' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'BOOK' AND a.code = 'BORROW';

INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/loans/me' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'MY_LOAN' AND a.code = 'READ';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'POST', '/api/loans/{id}/return' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'MY_LOAN' AND a.code = 'RETURN';

INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/loan-management' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'LOAN_MANAGE' AND a.code = 'READ';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'POST', '/api/loan-management' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'LOAN_MANAGE' AND a.code = 'CHECKOUT';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'POST', '/api/loan-management/{id}/return' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'LOAN_MANAGE' AND a.code = 'RETURN';

INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/books' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'BOOK_MANAGE' AND a.code = 'READ';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/books/{id}' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'BOOK_MANAGE' AND a.code = 'READ';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/books/categories' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'BOOK_MANAGE' AND a.code = 'READ';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'POST', '/api/books' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'BOOK_MANAGE' AND a.code = 'CREATE';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'PUT', '/api/books/{id}' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'BOOK_MANAGE' AND a.code = 'UPDATE';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'DELETE', '/api/books/{id}' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'BOOK_MANAGE' AND a.code = 'DELETE';

INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/admin/users' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'USER_MANAGE' AND a.code = 'READ';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/admin/roles' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'USER_MANAGE' AND a.code = 'READ';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'PUT', '/api/admin/users/{id}/role' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'USER_MANAGE' AND a.code = 'CHANGE_ROLE';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'POST', '/api/admin/users/{id}/unlock' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'USER_MANAGE' AND a.code = 'UNLOCK';

INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/admin/roles' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'ROLE_MANAGE' AND a.code = 'READ';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/admin/menus' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'ROLE_MANAGE' AND a.code = 'READ';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/admin/roles/{id}/actions' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'ROLE_MANAGE' AND a.code = 'READ';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'PUT', '/api/admin/roles/{id}/actions' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'ROLE_MANAGE' AND a.code = 'GRANT';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'POST', '/api/admin/actions/{id}/urls' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'ROLE_MANAGE' AND a.code = 'URL_EDIT';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'DELETE', '/api/admin/action-urls/{id}' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'ROLE_MANAGE' AND a.code = 'URL_EDIT';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'POST', '/api/admin/authz/reload' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'ROLE_MANAGE' AND a.code = 'URL_EDIT';

INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/admin/access-conditions' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'ACCESS_CONDITION' AND a.code = 'READ';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/admin/access-conditions/{userId}' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'ACCESS_CONDITION' AND a.code = 'READ';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'PUT', '/api/admin/access-conditions/{userId}' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'ACCESS_CONDITION' AND a.code = 'SAVE';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'DELETE', '/api/admin/access-conditions/{userId}' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'ACCESS_CONDITION' AND a.code = 'DELETE';

INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/admin/api-keys' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'API_KEY' AND a.code = 'READ';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'GET', '/api/admin/menus' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'API_KEY' AND a.code = 'READ';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'POST', '/api/admin/api-keys' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'API_KEY' AND a.code = 'ISSUE';
INSERT INTO action_urls (action_id, http_method, url_pattern) SELECT a.id, 'POST', '/api/admin/api-keys/{id}/revoke' FROM menu_actions a JOIN menus m ON m.id = a.menu_id WHERE m.code = 'API_KEY' AND a.code = 'REVOKE';

-- ---------------------------------------------------------------------
-- 로그인만 필요한 URL
-- ---------------------------------------------------------------------
INSERT INTO authenticated_urls (http_method, url_pattern, description) VALUES ('GET', '/api/me', '내 정보');
INSERT INTO authenticated_urls (http_method, url_pattern, description) VALUES ('GET', '/api/me/permissions', '내 권한(호출 가능 URL·메뉴)');

-- ---------------------------------------------------------------------
-- 역할별 액션 부여
-- ADMIN: 전 액션 / LIBRARIAN: 대시보드·도서 관리·대출 처리 / MEMBER: 도서 조회·대출 신청·내 대출·반납
-- (LIBRARIAN 은 BOOK:READ 없이 BOOK_MANAGE:READ 로 같은 도서 조회 URL 을 호출한다 — OR 규칙)
-- ---------------------------------------------------------------------
INSERT INTO role_actions (role_id, action_id) SELECT r.id, a.id FROM roles r CROSS JOIN menu_actions a WHERE r.code = 'ADMIN';

INSERT INTO role_actions (role_id, action_id) SELECT r.id, a.id FROM roles r CROSS JOIN menu_actions a JOIN menus m ON m.id = a.menu_id WHERE r.code = 'LIBRARIAN' AND m.code IN ('DASHBOARD', 'LOAN_MANAGE', 'BOOK_MANAGE');

INSERT INTO role_actions (role_id, action_id) SELECT r.id, a.id FROM roles r CROSS JOIN menu_actions a JOIN menus m ON m.id = a.menu_id WHERE r.code = 'MEMBER' AND m.code IN ('BOOK', 'MY_LOAN');

-- ---------------------------------------------------------------------
-- 샘플 계정 (개발용 비밀번호는 README 참고, BCrypt 해시로 저장)
-- admin / Admin123!, librarian / Librarian123!, member / Member123!
-- ---------------------------------------------------------------------
INSERT INTO users (username, password, name, email, role_id) SELECT 'admin', '$2a$10$5WMuK/fW.eaF9a6JWxS/EuUg2yKO5EssJvDT0vnJfNeWMpWvxswl6', '관리자', 'admin@library.local', id FROM roles WHERE code = 'ADMIN';
INSERT INTO users (username, password, name, email, role_id) SELECT 'librarian', '$2a$10$W/4e5VIEJavMm7YiTIpfMeAswlbIAamM.DLGRDu5KKb2xPM.prCsO', '김사서', 'librarian@library.local', id FROM roles WHERE code = 'LIBRARIAN';
INSERT INTO users (username, password, name, email, role_id) SELECT 'member', '$2a$10$GMsE02sg7fz0PO4CKz1Z4OGbuG.Q.4LvNfsR.DznNxPI1J9nQPr/y', '이회원', 'member@library.local', id FROM roles WHERE code = 'MEMBER';

-- ---------------------------------------------------------------------
-- 도서
-- ---------------------------------------------------------------------
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9788966260959', '클린 코드', '로버트 C. 마틴', '인사이트', 'IT', 3, 2);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9788966262472', '클린 아키텍처', '로버트 C. 마틴', '인사이트', 'IT', 2, 2);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9791162242025', '리팩터링 2판', '마틴 파울러', '한빛미디어', 'IT', 2, 2);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9788994492032', '자바 ORM 표준 JPA 프로그래밍', '김영한', '에이콘출판', 'IT', 3, 3);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9788966263158', '오브젝트', '조영호', '위키북스', 'IT', 2, 2);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9791158391409', '토비의 스프링 3.1', '이일민', '에이콘출판', 'IT', 1, 1);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9788968481475', '이펙티브 자바 3판', '조슈아 블로크', '인사이트', 'IT', 2, 2);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9791189909178', '데이터 중심 애플리케이션 설계', '마틴 클레프만', '위키북스', 'IT', 1, 1);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9788936434120', '소년이 온다', '한강', '창비', '소설', 3, 3);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9788936433598', '채식주의자', '한강', '창비', '소설', 2, 2);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9788954651134', '아몬드', '손원평', '창비', '소설', 2, 2);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9788937460449', '데미안', '헤르만 헤세', '민음사', '소설', 2, 2);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9788937462788', '1984', '조지 오웰', '민음사', '소설', 2, 2);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9788934972464', '사피엔스', '유발 하라리', '김영사', '인문', 3, 3);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9788932473901', '정의란 무엇인가', '마이클 샌델', '와이즈베리', '인문', 1, 1);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9788983711892', '총, 균, 쇠', '재레드 다이아몬드', '문학사상', '인문', 2, 2);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9788901219943', '코스모스', '칼 세이건', '사이언스북스', '과학', 2, 2);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9788932917245', '이기적 유전자', '리처드 도킨스', '을유문화사', '과학', 2, 2);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9788983719478', '시간의 역사', '스티븐 호킹', '까치', '과학', 1, 1);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9791190030090', '돈의 속성', '김승호', '스노우폭스북스', '경제', 2, 2);
INSERT INTO books (isbn, title, author, publisher, category, total_quantity, available_quantity) VALUES ('9788934985051', '넛지', '리처드 탈러', '리더스북', '경제', 1, 1);

-- ---------------------------------------------------------------------
-- 샘플 대출 (member 가 클린 코드 1권 대출 중 — 위 available_quantity 에 반영됨)
-- ---------------------------------------------------------------------
INSERT INTO loans (user_id, book_id, loan_date, due_date, status) SELECT u.id, b.id, CAST(CURRENT_DATE - INTERVAL '3' DAY AS DATE), CAST(CURRENT_DATE + INTERVAL '11' DAY AS DATE), 'LOANED' FROM users u CROSS JOIN books b WHERE u.username = 'member' AND b.isbn = '9788966260959';

-- ---------------------------------------------------------------------
-- 샘플 API Key (개발용: 외부 도서 조회) — 원문은 README 참고, DB 에는 SHA-256 해시만 저장
-- ---------------------------------------------------------------------
INSERT INTO api_keys (name, key_prefix, key_hash, owner_user_id) SELECT '외부 도서 조회 샘플', 'lib_Elsu', '565cdfe9b369ac3546605edfaeeca71b4f125b53ddf2d1c6dd488aad7116fde9', id FROM users WHERE username = 'admin';
INSERT INTO api_key_actions (api_key_id, action_id) SELECT k.id, a.id FROM api_keys k CROSS JOIN menu_actions a JOIN menus m ON m.id = a.menu_id WHERE k.key_prefix = 'lib_Elsu' AND m.code = 'BOOK' AND a.code = 'READ';
