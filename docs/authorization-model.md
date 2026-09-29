# 인가 모델 — 메뉴 · 액션 · URL

## 1. 모델

```
역할(roles) ──< role_actions >── 액션(menu_actions) ──< action_urls (METHOD + URL 패턴)
                                      │
API Key(api_keys) ──< api_key_actions >┘
                                      │
                                 메뉴(menus)
```

| 개념 | 설명 | 예 |
|---|---|---|
| 메뉴 | 화면 단위 | `BOOK_MANAGE`(도서 관리) |
| 액션 | 메뉴 안의 권한 단위. `READ`(화면 진입·조회) 또는 `ACTION`(버튼 동작) | `BOOK_MANAGE:READ`, `BOOK_MANAGE:CREATE` |
| 액션 URL | 액션이 허용하는 HTTP 메서드 + URL 패턴(여러 개) | `POST /api/books` |
| 역할 | 액션 묶음. 사용자는 역할 1개 | `LIBRARIAN` |
| API Key | 사람이 아닌 호출자. 역할 대신 액션을 직접 부여 | `BOOK:READ` 만 가진 외부 조회용 키 |

- **메뉴 진입**은 그 메뉴의 `READ` 액션 보유로 판정한다(사이드바 표시 기준).
- **authenticated_urls**: 로그인(사용자)만 되어 있으면 되는 URL. 예: `GET /api/me`, `GET /api/me/permissions`. API Key 로는 호출할 수 없다.
- **permitAll**: 로그인·회원가입·토큰 재발급·로그아웃·Swagger. 테이블이 아니라 `PublicEndpoints` 코드 한 곳에서만 관리한다.
  (공개 URL 을 DB 로 두면 관리 화면 실수 한 번으로 보호 API 가 공개될 수 있으므로, 배포(코드 리뷰)를 거쳐야만 바뀌게 했다.)

## 2. 판정 규칙

`UrlAuthorizationManager` 하나가 permitAll 을 제외한 모든 요청을 판정한다.

1. 인증 주체가 없으면 거부 → **401**
2. 기동 시 `action_urls` + `authenticated_urls` 를 읽어 (메서드, 패턴)별 규칙 테이블을 만든다.
   같은 (메서드, 패턴)이 여러 액션에 걸리면 한 규칙으로 합치고 액션 코드를 모은다.
3. 요청 메서드의 규칙들을 `PathPattern.SPECIFICITY_COMPARATOR` 로 정렬해 두고, **가장 구체적인 규칙 하나**만 쓴다.
   - `/api/books/categories`(리터럴) 가 `/api/books/{id}`(변수) 보다, `/api/admin/users` 가 `/api/admin/**` 보다 우선한다.
4. **일치하는 규칙이 없으면 거부(fail-closed) → 403.** 새 API 를 만들고 등록을 잊으면 열리는 게 아니라 막힌다.
5. `authenticated_urls` 규칙이면 사용자 주체는 통과.
6. 그 외에는 규칙의 액션 중 **하나라도** 보유하면 통과(**OR**).

### OR 규칙 예

`GET /api/books` 는 `BOOK:READ`(도서 목록 메뉴)와 `BOOK_MANAGE:READ`(도서 관리 메뉴) 양쪽에 등록돼 있다.
사서는 `BOOK:READ` 가 없지만 `BOOK_MANAGE:READ` 로 같은 URL 을 호출한다. 같은 방식으로
`GET /api/admin/roles` 는 회원 관리·역할 관리, `GET /api/admin/menus` 는 역할 관리·API Key 관리에서 함께 쓴다.

### 누락 방지 장치

- `MappingCoverageTest` — `RequestMappingHandlerMapping` 의 모든 컨트롤러 매핑이 action_urls / authenticated_urls / permitAll 중
  하나에 있는지, 반대로 등록된 규칙이 실제 매핑을 가리키는지 대조한다. 어긋나면 `gradlew build` 가 실패한다.
- `npm run check:can` — 프론트 `<Can url="...">`·`can("...")` 리터럴이 `data.sql` 의 action_urls 시드에 있는지 검사한다.

## 3. 프론트엔드 권한 표시 — URL 로 묻는다

`GET /api/me/permissions` 응답:

```json
{
  "urls": ["GET /api/books", "GET /api/books/{id}", "POST /api/loans", "POST /api/loans/{id}/return"],
  "menus": ["BOOK", "MY_LOAN"]
}
```

- `urls` 는 로그인 사용자가 호출 가능한 action_urls 의 `"METHOD 패턴"` 문자열 집합이다(등록된 패턴 그대로, 중복 제거).
- 버튼은 **자기가 부르는 주 동작 URL 하나**로 묻는다: `<Can url="POST /api/loans"><button>대출</button></Can>`.
- 프론트는 패턴 매칭을 하지 않는다. 등록 패턴 문자열과 **정확히 일치**하는지만 본다(`Set.has`).
- 사이드바는 `menus`(READ 보유 메뉴 코드)로 그린다.
- 403 을 받으면 권한이 바뀌었을 수 있으므로 `/api/me/permissions` 를 다시 받아오고 안내한다.

### 왜 액션 코드가 아니라 URL 인가 (설계 근거)

버튼이 `BOOK_MANAGE:CREATE` 같은 **액션 코드**로 묻는다면, 관리자가 액션을 쪼개거나(예: `CREATE` → `CREATE_DOMESTIC`/`CREATE_FOREIGN`)
합치는 순간 프론트 코드의 코드 문자열도 함께 고쳐야 한다. 액션은 **권한을 묶는 운영 단위**라 자주 바뀌지만,
버튼이 호출하는 **URL 은 API 계약**이라 거의 바뀌지 않는다.

- 버튼이 URL 로 물으면, 액션 구성을 어떻게 바꾸든 "이 사용자가 그 URL 을 부를 수 있는가"는 서버가 계산해 준다.
  → **액션을 쪼개거나 합쳐도 프론트는 고칠 필요가 없다.**
- 버튼 표시와 서버 판정이 같은 원천(action_urls)에서 나오므로, "버튼은 보이는데 누르면 403" 이 구조적으로 줄어든다.
- 트레이드오프: 한 버튼이 여러 URL 을 연달아 부르면 주 동작 URL 하나로만 묻기 때문에 나머지 URL 권한은 표시에 반영되지 않는다.
  (최종 차단은 항상 서버가 한다.) 또 URL 문자열 오타는 조용히 버튼을 숨기므로 `check:can` 스크립트로 막는다.
- 프론트에 전체 호출 가능 URL 목록이 노출되지만, 이는 이미 그 사용자가 호출할 수 있는 URL 이므로 새로운 정보 노출은 아니다.

## 4. API Key 흐름

```
요청 ─ X-API-KEY 헤더? ─ 아니오 → JWT 필터로
          │ 예
          ├─ 이 IP 의 이번 분 인증 실패가 한도 도달 → 429 TOO_MANY_REQUESTS (조회하지 않음)
          ├─ 형식(lib_ + 43자 base64url) 불일치 → 401 INVALID_API_KEY
          ├─ SHA-256 해시로 캐시/음성 캐시/DB 조회 → 없음·폐기·만료 → 401 INVALID_API_KEY   (JWT 로 폴백하지 않음)
          │     (위 두 401 은 IP 별 실패 횟수에 더한다. DB 에 없는 해시는 60초간 음성 캐시)
          └─ 인증 성공(ApiKeyPrincipal: 부여 액션·허용 IP)
                 → 접속 조건 필터: api_key_allowed_ips 불일치 → 403 ACCESS_CONDITION_DENIED
                 → UrlAuthorizationManager: api_key_actions 로 같은 규칙 판정
```

- 발급: `SecureRandom` 32바이트 → `lib_` + base64url(패딩 없음). 원문은 **발급 응답에서 1회만** 주고 DB 에는 SHA-256 해시와 표시용 앞 8자만 저장한다.
  키가 충분히 길고 무작위이므로 BCrypt 같은 느린 해시가 필요 없고, 해시를 곧바로 조회 키(UNIQUE)로 쓸 수 있다.
- **권한 상승 방지**: 발급자는 자신이 보유한 액션만 키에 부여할 수 있다. 역할 권한 저장·역할 변경도 같은 원칙을 따르고,
  자기 역할의 권한과 그 액션의 URL 은 편집할 수 없다([architecture.md §6](architecture.md#6-주요-설계-결정과-트레이드오프)).
- **사용자 전용 API**: "본인" 이 필요한 API(`/api/loans` 대출·내 대출·반납, `/api/me`, API Key 발급, 회원 역할 변경)는 API Key 로 부르면
  URL 인가를 통과하더라도 **403 `USER_ONLY`** 다(주체가 사용자가 아니므로).
- 폐기하면 커밋 후 해당 키 캐시가 evict 되어 다음 요청부터 401 이다.
- 없는 키 반복 조회·무차별 대입 방어(음성 캐시, IP 별 1분 실패 한도 → 429)는 [architecture.md §4](architecture.md#api-key-무차별-대입db-부하-방어) 참고.
- `last_used_at` 은 노드별로 1분에 한 번만 갱신해 호출마다 UPDATE 가 나가지 않게 했다.
- 시드: `외부 도서 조회 샘플` 키(`BOOK:READ` — `GET /api/books`, `GET /api/books/{id}`, `GET /api/books/categories`).

## 5. 사용자 접속 조건

`user_access_conditions` 한 행(사용자당 0..1). 비어 있는 항목은 제한하지 않는다.

| 항목 | 규칙 |
|---|---|
| allowed_ips | 콤마 구분 단일 IP 또는 CIDR(IPv4/IPv6). 하나라도 일치하면 통과 |
| valid_from / valid_to | 시작일·종료일 **당일 포함** |
| allowed_days | `MON,TUE,...` 중 오늘 요일 포함 |
| start_time / end_time | 경계 포함. 시작 > 종료이면 자정을 넘는 구간(예: 22:00~06:00) |

- 인증된 요청마다 `AccessConditionFilter` 가 검사한다. 위반 시 **403 `ACCESS_CONDITION_DENIED`** — 구체 사유(IP/기간/요일/시간)는 응답에 주지 않고 로그에만 남긴다(IP 는 마스킹).
- 로그인(`/api/auth/login`)·재발급(`/api/auth/refresh`) 시에도 같은 판정기로 검사해, 조건 밖에서는 토큰 자체를 발급하지 않는다(같은 403 코드).
  로그인 실패 횟수에는 넣지 않는다. 프론트 로그인 화면은 "허용된 접속 환경이 아닙니다. 관리자에게 문의하세요." 를 표시한다.
- 클라이언트 IP 는 `request.getRemoteAddr()` 기준이다. `X-Forwarded-For` 는 직전 홉이 `app.security.trusted-proxies` 에
  등록된 프록시일 때만 보며, 오른쪽(가까운 홉)부터 거슬러 첫 번째 비신뢰 주소를 클라이언트로 본다(왼쪽 값 위조 방지).
- IP 값은 저장 전에 엄격히 검증한다. Spring 의 `IpAddressMatcher` 는 IP 가 아닌 문자열을 호스트명으로 보고 DNS 조회를 시도하기 때문이다.
- 시간대는 `app.zone-id`(기본 `Asia/Seoul`) 기준이다.
