# API 명세

- 기본 URL: `http://localhost:8080` (프론트 개발 서버에서는 `/api` 가 프록시된다)
- 인증: `Authorization: Bearer <accessToken>` 또는 `X-API-KEY: <key>`
- "필요 권한" 은 `data.sql` 시드 기준이다. 관리 화면에서 URL 매핑을 바꾸면 달라질 수 있다.
  여러 액션이 적힌 경우 **그중 하나만 보유해도 통과(OR)**.
- Swagger UI: `/swagger-ui.html` (permitAll)
- 오류 응답 형식은 [architecture.md §7](architecture.md#7-오류-응답) 참고.

## 인증 (permitAll)

| 메서드 | URL | 설명 |
|---|---|---|
| POST | `/api/auth/signup` | 회원가입(일반 회원 역할 자동 부여) → 201 |
| POST | `/api/auth/login` | 로그인 → Access(15분) + Refresh(7일) |
| POST | `/api/auth/refresh` | Refresh 토큰 회전 재발급 |
| POST | `/api/auth/logout` | Refresh 토큰 폐기 → 204 |

```http
POST /api/auth/signup
{ "username": "newbie01", "password": "Passw0rd!", "name": "신입", "email": "newbie@library.local" }

201 { "id": 4, "username": "newbie01" }
400 { "code": "VALIDATION_FAILED", "fieldErrors": [ { "field": "password", "message": "비밀번호는 영문·숫자·특수문자를 모두 포함해야 합니다." } ], ... }
409 { "code": "DUPLICATE_USERNAME", ... }
```

```http
POST /api/auth/login
{ "username": "member", "password": "Member123!" }

200 { "accessToken": "eyJ...", "refreshToken": "q3Jx...", "tokenType": "Bearer", "expiresIn": 900 }
401 { "code": "INVALID_CREDENTIALS" }   // 5회 연속 실패 시 { "code": "ACCOUNT_LOCKED" }
```

```http
POST /api/auth/refresh
{ "refreshToken": "q3Jx..." }

200 { "accessToken": "...", "refreshToken": "(새 토큰)", ... }
401 { "code": "INVALID_REFRESH_TOKEN" }  // 만료·폐기·재사용(재사용이면 그 사용자 토큰 전체 폐기)
```

## 내 정보 (authenticated_urls — 로그인만 필요, API Key 불가)

| 메서드 | URL | 설명 |
|---|---|---|
| GET | `/api/me` | 내 정보 |
| GET | `/api/me/permissions` | 호출 가능 URL·진입 가능 메뉴 |

```http
GET /api/me/permissions
200 {
  "urls": ["GET /api/books", "GET /api/books/categories", "GET /api/books/{id}",
           "GET /api/loans/me", "POST /api/loans", "POST /api/loans/{id}/return"],
  "menus": ["BOOK", "MY_LOAN"]
}
```

## 도서

| 메서드 | URL | 필요 권한 |
|---|---|---|
| GET | `/api/books?keyword=&category=&page=0&size=10` | `BOOK:READ` 또는 `BOOK_MANAGE:READ` |
| GET | `/api/books/{id}` | `BOOK:READ` 또는 `BOOK_MANAGE:READ` |
| GET | `/api/books/categories` | `BOOK:READ` 또는 `BOOK_MANAGE:READ` |
| POST | `/api/books` | `BOOK_MANAGE:CREATE` |
| PUT | `/api/books/{id}` | `BOOK_MANAGE:UPDATE` |
| DELETE | `/api/books/{id}` | `BOOK_MANAGE:DELETE` |

```http
GET /api/books?keyword=클린&size=2
200 { "content": [ { "id": 2, "isbn": "9788966262472", "title": "클린 아키텍처", "author": "로버트 C. 마틴",
                     "publisher": "인사이트", "category": "IT", "totalQuantity": 2, "availableQuantity": 2 } ],
      "page": 0, "size": 2, "totalElements": 2, "totalPages": 1 }

POST /api/books
{ "isbn": "9780000000001", "title": "새 책", "author": "저자", "publisher": "출판사", "category": "IT", "totalQuantity": 2 }
201 { "id": 22, ..., "availableQuantity": 2 }
400 { "code": "INVALID_QUANTITY" }         // 보유 수량 < 대출 중 권수
409 { "code": "BOOK_HAS_ACTIVE_LOANS" }    // 삭제 시 대출 중
```

외부 조회는 API Key 로도 가능하다(시드 키는 `BOOK:READ`):

```bash
curl -H "X-API-KEY: lib_Elsu1z3_KkwNJYk7v7R7BxLaFBJpJV4qc61GHDjZvNY" http://localhost:8080/api/books/1
```

## 대출 (회원 본인)

| 메서드 | URL | 필요 권한 |
|---|---|---|
| POST | `/api/loans` | `BOOK:BORROW` |
| GET | `/api/loans/me` | `MY_LOAN:READ` |
| POST | `/api/loans/{id}/return` | `MY_LOAN:RETURN` (본인 대출만, 남의 것은 404) |

```http
POST /api/loans
{ "bookId": 2 }
201 { "id": 7, "bookId": 2, "bookTitle": "클린 아키텍처", "userId": 3, "username": "member",
      "loanDate": "2026-09-29", "dueDate": "2026-10-13", "returnedDate": null, "status": "LOANED" }
409 { "code": "LOAN_LIMIT_EXCEEDED" }   // 미반납 5권
409 { "code": "OVERDUE_LOAN_EXISTS" }   // 연체 중
409 { "code": "ALREADY_BORROWED" }      // 같은 책 대출 중
409 { "code": "BOOK_NOT_AVAILABLE" }    // 재고 없음
```

## 대출 관리 (사서)

| 메서드 | URL | 필요 권한 |
|---|---|---|
| GET | `/api/loan-management?status=&keyword=&page=&size=` | `LOAN_MANAGE:READ` |
| POST | `/api/loan-management` | `LOAN_MANAGE:CHECKOUT` |
| POST | `/api/loan-management/{id}/return` | `LOAN_MANAGE:RETURN` |

```http
POST /api/loan-management
{ "username": "member", "bookId": 5 }
201 { ... 대출 응답 ... }
```

## 대시보드

| 메서드 | URL | 필요 권한 |
|---|---|---|
| GET | `/api/dashboard/stats` | `DASHBOARD:READ` |

```http
200 { "titles": 21, "totalCopies": 41, "availableCopies": 40, "activeLoans": 1, "overdueLoans": 0, "users": 3 }
```

## 회원 관리

| 메서드 | URL | 필요 권한 |
|---|---|---|
| GET | `/api/admin/users?keyword=&page=&size=` | `USER_MANAGE:READ` |
| GET | `/api/admin/roles` | `USER_MANAGE:READ` 또는 `ROLE_MANAGE:READ` |
| PUT | `/api/admin/users/{id}/role` | `USER_MANAGE:CHANGE_ROLE` |
| POST | `/api/admin/users/{id}/unlock` | `USER_MANAGE:UNLOCK` |

```http
PUT /api/admin/users/4/role
{ "roleId": 2 }
200 { "id": 4, "username": "newbie01", "roleCode": "LIBRARIAN", ... }   // 기존 토큰에도 다음 요청부터 반영
400 { "code": "CANNOT_CHANGE_OWN_ROLE" }
```

## 역할·권한 관리

| 메서드 | URL | 필요 권한 |
|---|---|---|
| GET | `/api/admin/menus` | `ROLE_MANAGE:READ` 또는 `API_KEY:READ` |
| GET | `/api/admin/roles/{id}/actions` | `ROLE_MANAGE:READ` |
| PUT | `/api/admin/roles/{id}/actions` | `ROLE_MANAGE:GRANT` |
| POST | `/api/admin/actions/{id}/urls` | `ROLE_MANAGE:URL_EDIT` |
| DELETE | `/api/admin/action-urls/{id}` | `ROLE_MANAGE:URL_EDIT` |
| POST | `/api/admin/authz/reload` | `ROLE_MANAGE:URL_EDIT` |

```http
GET /api/admin/menus
200 [ { "id": 1, "code": "DASHBOARD", "name": "대시보드", "path": "/dashboard", "sortOrder": 10,
        "actions": [ { "id": 1, "code": "READ", "name": "대시보드 조회", "actionType": "READ",
                       "authorityCode": "DASHBOARD:READ",
                       "urls": [ { "id": 1, "httpMethod": "GET", "urlPattern": "/api/dashboard/stats" } ] } ] }, ... ]

PUT /api/admin/roles/3/actions
{ "actionIds": [2, 3, 4, 5, 1] }
200 { "roleId": 3, "actionIds": [1, 2, 3, 4, 5] }
400 { "code": "SYSTEM_ROLE_PROTECTED" }   // ADMIN 역할

POST /api/admin/actions/6/urls
{ "httpMethod": "GET", "urlPattern": "/api/admin/users" }
201 { "id": 38, "httpMethod": "GET", "urlPattern": "/api/admin/users" }   // 커밋 후 전 노드 규칙 재적재
```

URL 패턴은 리터럴 세그먼트, `{변수}`, `*`, 끝의 `/**` 만 허용한다.

## 접속 조건 관리

| 메서드 | URL | 필요 권한 |
|---|---|---|
| GET | `/api/admin/access-conditions?keyword=&page=&size=` | `ACCESS_CONDITION:READ` |
| GET | `/api/admin/access-conditions/{userId}` | `ACCESS_CONDITION:READ` |
| PUT | `/api/admin/access-conditions/{userId}` | `ACCESS_CONDITION:SAVE` |
| DELETE | `/api/admin/access-conditions/{userId}` | `ACCESS_CONDITION:DELETE` |

```http
PUT /api/admin/access-conditions/3
{ "allowedIps": ["192.168.0.0/24", "10.0.0.5"], "validFrom": "2026-01-01", "validTo": null,
  "allowedDays": ["MON","TUE","WED","THU","FRI"], "startTime": "09:00", "endTime": "18:00" }
200 { "userId": 3, "configured": true, ... }

// 조건 밖에서 호출하면
403 { "code": "ACCESS_CONDITION_DENIED", "message": "허용된 접속 조건이 아닙니다." }
```

## API Key 관리

| 메서드 | URL | 필요 권한 |
|---|---|---|
| GET | `/api/admin/api-keys` | `API_KEY:READ` |
| POST | `/api/admin/api-keys` | `API_KEY:ISSUE` (본인 보유 액션만 부여 가능) |
| POST | `/api/admin/api-keys/{id}/revoke` | `API_KEY:REVOKE` |

```http
POST /api/admin/api-keys
{ "name": "외부 카탈로그", "actionIds": [2], "allowedIps": [], "expiresAt": "2026-12-31T23:59:00" }
201 { "apiKey": "lib_x9...(원문, 이 응답에서만)", "detail": { "id": 2, "keyPrefix": "lib_x9Ab", "status": "ACTIVE", ... } }

POST /api/admin/api-keys/2/revoke
200 { "id": 2, "status": "REVOKED", ... }   // 이후 이 키로 호출하면 401 INVALID_API_KEY
```
