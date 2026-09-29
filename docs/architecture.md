# 아키텍처 — 인증·인가 흐름과 설계 결정

## 1. 구성

```
[브라우저: React + Vite] ── /api 프록시 ──> [Spring Boot 3.5 (Security 6.5)] ──> [H2(기본) / PostgreSQL]
                                                    │
                                                    └── Hazelcast(embedded) 권한 캐시 — 노드 간 TCP/IP 클러스터
```

- 백엔드: Java 21, Spring Boot 3.5, Spring Security 6.5, Spring Data JPA, jjwt 0.12, Hazelcast 5.5, springdoc.
- 스키마·시드: `schema.sql` + `data.sql` (JPA 는 `ddl-auto=validate` 로 매핑 검증만).
- 프론트: React 18 + TypeScript + Vite, react-router, axios. UI 라이브러리 없이 순수 CSS.

## 2. 필터 체인 순서

```
요청
 │
 ├─ ApiKeyAuthenticationFilter     X-API-KEY 가 있으면 검증. 무효면 즉시 401(JWT 폴백 없음)
 │                                  (IP 별 실패 한도 초과 시 조회 없이 429 TOO_MANY_REQUESTS)
 ├─ JwtAuthenticationFilter        Bearer 토큰 검증 → userId 로 캐시에서 역할·잠금 조회
 │                                  (무효·만료면 인증하지 않고 사유만 기록 → 보호 URL 이면 401 TOKEN_EXPIRED 등)
 ├─ AccessConditionFilter          인증된 주체의 접속 조건 검사. 위반 → 403 ACCESS_CONDITION_DENIED
 ├─ (Spring) ExceptionTranslationFilter
 └─ AuthorizationFilter
      ├─ dispatcherType=ERROR      permitAll
      ├─ PublicEndpoints           permitAll (로그인·가입·재발급·로그아웃·Swagger)
      └─ anyRequest                UrlAuthorizationManager (fail-closed)
```

- 401 은 `JsonAuthenticationEntryPoint`, 403 은 `JsonAccessDeniedHandler` **한 곳에서만** JSON 으로 만든다.
  필터 단계 실패(API Key 무효, 접속 조건 위반)도 이 두 처리기를 직접 호출한다.
- 커스텀 필터는 스프링 빈으로 등록하지 않고 `SecurityConfig` 에서 생성한다(서블릿 필터로 이중 등록 방지).
- 세션을 쓰지 않는 Bearer 토큰 API 이므로 CSRF 는 끈다.

## 3. 인증

### 로그인·토큰

| 항목 | 값 |
|---|---|
| Access JWT | 15분, HMAC 서명, **subject = userId 만** |
| Refresh 토큰 | 32바이트 무작위, DB 에는 SHA-256 해시만, 7일 |
| 재발급 | 회전(rotation): 기존 토큰 폐기 + 새 토큰 발급 |
| 재사용 탐지 | 이미 폐기된 Refresh 토큰이 오면 탈취로 보고 그 사용자의 토큰을 모두 폐기. 소비는 조건부 UPDATE(`revoked_at is null`)로 원자화해 같은 토큰을 **동시에** 제시해도 하나만 성공하고 나머지는 재사용으로 처리(전체 폐기 + 401) |
| 로그아웃 | 제시된 Refresh 토큰 폐기(멱등) |
| 비밀번호 | BCrypt |
| 잠금 | 연속 5회 실패 시 잠금, 관리자만 해제. 잠기면 기존 Access 토큰도 다음 요청부터 401 `ACCOUNT_LOCKED`. 잠기는 순간 그 사용자의 Refresh 토큰도 모두 폐기한다. 로그인 응답의 `ACCOUNT_LOCKED` 는 **비밀번호가 맞을 때만** 준다(틀리면 잠금 여부와 무관하게 `INVALID_CREDENTIALS`) |
| IP 단위 제한 | 클라이언트 IP 별 1분 로그인 실패 한도(`app.security.login.max-failures-per-minute`, 기본 30) 도달 시 그 분 동안 429 `TOO_MANY_REQUESTS` — 여러 계정에 비밀번호를 뿌리는 시도 억제 |
| 접속 조건 | 로그인·재발급 시에도 검사. 비밀번호가 맞아도 조건(IP·기간·요일·시간) 밖이면 토큰을 주지 않고 403 `ACCESS_CONDITION_DENIED` |

- 없는 아이디도 더미 해시와 비교해 응답 시간으로 계정 존재 여부가 드러나지 않게 했다.
- 실패 횟수는 로그인 트랜잭션과 분리해 로그인이 예외로 끝나도 커밋된다. 증가(`failed_login_count + 1`)와 잠금 판정(`failed_login_count >= 한도` 조건부 UPDATE)은
  모두 DB 원자 연산이라 틀린 비밀번호를 동시에 여러 번 보내도 횟수가 유실되지 않는다.
- 접속 조건 판정은 `AccessConditionFilter` 와 같은 `AccessConditionEvaluator` 를 쓰고, 응답도 같은 코드·형식이다(구체 사유는 로그에만).
  비밀번호 확인 **뒤에** 검사하며 로그인 실패 횟수는 올리지 않는다(조건 밖에서 반복 로그인해도 계정이 잠기지 않는다).
  재발급은 제시된 Refresh 토큰을 소비한 뒤 판정하므로, 조건 밖에서 거절된 토큰은 다시 쓸 수 없다 — 조건 안에서 다시 로그인해야 한다.

### 권한을 토큰에 넣지 않는 이유

JWT 에 역할·권한을 넣으면 관리자가 권한을 회수해도 토큰 만료(15분)까지 옛 권한이 살아 있다.
토큰에는 userId 만 넣고, **매 요청 캐시에서** 역할·잠금·접속 조건을 읽는다. 캐시는 변경 커밋 직후 해당 키만 evict 되므로
권한 회수·잠금·접속 조건 변경이 **다음 요청부터 즉시** 적용된다. 대가는 요청마다 캐시 조회 1~2회다.

## 4. 캐시와 evict

| IMap | 키 → 값 | evict 시점 |
|---|---|---|
| `user-auth` | userId → 역할 ID·잠금·접속 조건 | 역할 변경, 잠금/해제, 접속 조건 저장·삭제 |
| `role-actions` | roleId → 보유 액션 코드 집합 | 역할 권한 저장 |
| `api-keys` | 키 해시 → 부여 액션·허용 IP·만료·폐기 | API Key 발급·폐기 |
| `api-key-misses` | DB 에 없는 키 해시 → 표시(음성 캐시, 항목 TTL 60초) | API Key 발급·폐기(같은 해시) |
| `api-key-failures` | "IP\|분" → API Key 인증 실패 횟수(TTL 120초) | 자연 만료 |
| `authz-generations` | "user:ID"·"role:ID"·"apikey:해시" → evict 세대 번호 | evict 때 증가(TTL 600초) |
| `login-failures` | "IP\|분" → 로그인 실패 횟수(TTL 120초) | 자연 만료 |

- **캐시 미스면 DB 에서 읽어 채운다. 미스를 통과로 취급하지 않는다.**
- evict 는 `@TransactionalEventListener(AFTER_COMMIT)` 에서만 한다. 커밋 전에 지우면 다른 요청이 옛 DB 값을 다시 적재할 수 있다.
- 값 객체는 `Serializable`(serialVersionUID 명시). `Optional` 은 직렬화되지 않으므로 필드는 nullable 로 두고 접근자에서 `Optional` 로 감싼다.
- 안전장치: 모든 IMap 에 TTL 600초. evict 가 어떤 이유로 누락돼도 오래된 권한이 무기한 남지 않는다.
- 적재·evict 경합: "캐시 미스 → 옛 DB 값 읽기 → (변경 커밋·evict) → 옛 값 저장" 순서가 되면 옛 권한이 TTL 까지 남을 수 있다.
  그래서 evict 는 키별 **세대 번호**(`authz-generations`, `EntryProcessor` 원자 증가)를 먼저 올린 뒤 값을 지우고, 적재는 DB 를 읽기 전 세대를 기억했다가
  값을 넣은 뒤 세대가 바뀌었으면 방금 넣은 값을 지운다. 경합한 그 요청 하나만 읽은 값으로 처리되고, 다음 요청부터는 새 값을 읽는다.

### API Key 무차별 대입·DB 부하 방어

형식은 맞지만 존재하지 않는 키를 대량으로 보내면 요청마다 DB 조회가 일어난다. 두 가지로 막는다.

1. **음성 캐시** — DB 에도 없는 해시는 `api-key-misses` 에 짧은 TTL(`app.security.api-key.negative-cache-ttl`, 기본 60초)로 두고,
   그동안 같은 해시는 DB 를 다시 조회하지 않고 바로 401 이다. 키를 발급·폐기하면 커밋 후 그 해시의 스냅샷과 음성 캐시를 함께 지운다
   (SHA-256 충돌은 사실상 없지만, 발급 직후 같은 해시가 "없음"으로 남는 경우를 규칙으로 차단).
   무작위 키를 매번 바꿔 보내는 공격에는 음성 캐시가 듣지 않으므로 아래 횟수 제한이 함께 필요하다.
2. **IP 별 실패 횟수 제한** — API Key 인증 실패(형식 오류·미등록·폐기·만료)를 클라이언트 IP(`ClientIpResolver` 기준) 별로
   분 단위 버킷(`IP|epoch분`)에 센다. 카운터는 Hazelcast IMap 에 `EntryProcessor` 로 원자적으로 더하므로 여러 노드가 같은 값을 본다.
   한 분 동안 실패가 한도(`app.security.api-key.max-failures-per-minute`, 기본 20)에 도달하면 **그 분이 끝날 때까지** 해당 IP 의
   `X-API-KEY` 요청은 캐시·DB 조회 없이 **429 `TOO_MANY_REQUESTS`**(`Retry-After`: 남은 초)로 거절한다.
   별도 라이브러리(Bucket4j 등)는 넣지 않았다.

트레이드오프
- 고정 창(분 경계)이라 경계 직전·직후에 몰아 보내면 짧은 순간 최대 2배(40회)까지 시도할 수 있다. 슬라이딩 창보다 단순한 대신의 한계다.
- 차단 중에는 같은 IP 의 **유효한 키도** 거절된다(NAT 뒤 여러 클라이언트가 한 IP 를 공유하면 함께 막힐 수 있음). 무차별 대입 억제를 우선했다.
- 클라이언트 IP 는 신뢰 프록시 설정을 따르므로, 프록시 뒤에서 `trusted-proxies` 를 빠뜨리면 모든 요청이 프록시 IP 하나로 세어진다.
- JWT 로그인은 같은 방식의 별도 카운터(`login-failures`, IP 별 1분 한도)와 계정별 5회 잠금으로 방어한다.

### 인가 규칙(action_urls) 재적재

규칙 테이블은 각 노드 메모리에 불변 객체로 있다(요청 처리 중 락 없이 읽기).
관리 화면에서 URL 매핑을 바꾸면 커밋 후 **변경 노드는 즉시(동기) 다시 적재**하고, Hazelcast 토픽 `authz-rules-reload` 로
다른 노드에 알려 각자 다시 적재하게 한다. 수동 재적재 API(`POST /api/admin/authz/reload`)도 같은 경로를 탄다.

## 5. 왜 로컬 캐시가 아니라 분산 캐시인가

| | 로컬 캐시(Caffeine 등) | 분산 캐시(Hazelcast embedded) |
|---|---|---|
| 단일 노드 | 가장 빠르고 단순 | 동작은 같지만 직렬화·멤버 관리 비용이 추가 |
| 다중 노드 권한 회수 | evict 가 **그 노드에만** 적용 → 다른 노드는 TTL 까지 옛 권한 | evict 한 번이 **모든 노드에** 적용 → 즉시 회수 |
| 운영 | 설정 없음 | 멤버 목록·포트·네트워크 설정 필요 |

이 프로젝트는 "권한 회수가 즉시 반영된다"를 핵심 요구로 두었고, 서버를 두 대 이상 띄우는 순간 로컬 캐시로는 이 요구를 지킬 수 없다
(노드 A 에서 회수해도 노드 B 에 캐시된 권한이 남는다). 그래서 캐시 자체를 클러스터가 공유하는 Hazelcast IMap 으로 두었다.

**트레이드오프 — 단일 노드에서는 오버엔지니어링일 수 있다.** 서버가 한 대뿐이면 Caffeine 같은 로컬 캐시가 더 빠르고 단순하다.
대안으로 "로컬 캐시 + 메시지(토픽·Redis Pub/Sub)로 evict 전파"도 있다 — 읽기 성능은 로컬 캐시만큼 좋지만 메시지 유실 시 일관성이
TTL 에 기대게 된다. 여기서는 설정으로 멤버를 늘리기만 하면 되는 단순함과 즉시 일관성을 우선했다.

설정: 멀티캐스트·자동 탐지는 끄고 TCP/IP 멤버 목록(`app.hazelcast.members`, 기본 `127.0.0.1`)으로 합류한다. 단일 노드로 그대로 뜬다.

> **보안 주의 — 오픈소스 Hazelcast 는 멤버 인증이 없어 반드시 사설망·방화벽 뒤에 둔다.**
> 클러스터 포트(기본 5701~)에 붙을 수 있으면 권한 캐시(`user-auth`, `api-keys` 등)를 읽고 바꿀 수 있다.
> 그래서 멤버는 **지정한 인터페이스에만 바인딩**한다(`hazelcast.socket.bind.any=false`, `app.hazelcast.interface`, 기본 `127.0.0.1`).
> 다중 노드는 `HAZELCAST_INTERFACE`(예: `10.0.0.*`)와 `HAZELCAST_MEMBERS` 를 사설 대역으로 지정하고, 방화벽으로 클러스터 포트를
> 애플리케이션 노드끼리만 열어 둔다. 루프백·사설 대역(10/8, 172.16/12, 192.168/16)이 아닌 인터페이스를 지정하면 기동에 실패한다.

```bash
# 같은 PC 에서 2노드 예시(두 번째 노드는 포트만 다르게, 루프백 바인딩 그대로)
java -jar library-backend.jar --server.port=8080
java -jar library-backend.jar --server.port=8081   # Hazelcast 포트는 5701 → 5702 자동 증가 후 합류

# 서로 다른 서버 2대(사설망) 예시
HAZELCAST_INTERFACE=10.0.0.* HAZELCAST_MEMBERS=10.0.0.11,10.0.0.12 java -jar library-backend.jar
```

> 주의: 기본 H2 는 **인메모리이며 노드마다 별개**다. 2노드 실제 운영 검증은 `postgres` 프로필로 같은 DB 를 바라보게 해야 한다.
> 2노드 동작은 `TwoNodeAuthzCacheTest`(같은 JVM 에 Hazelcast 멤버 2개)로 자동 검증한다:
> 노드 A 에서 역할 캐시 evict → 노드 B 가 곧바로 새 값을 읽음 / 노드 A 규칙 변경 알림 → 노드 B 규칙 재적재.

## 6. 주요 설계 결정과 트레이드오프

| 결정 | 이유 | 대가 |
|---|---|---|
| URL 기반 인가 + fail-closed | 새 API 가 등록 전까지 자동으로 막힘. 권한을 DB 로 운영 | 모든 매핑을 등록해야 함 → `MappingCoverageTest` 로 강제 |
| 가장 구체적인 규칙 하나만 사용 | `**` 같은 넓은 규칙이 좁은 규칙을 덮어쓰지 않음 | 넓은 규칙과 좁은 규칙의 액션을 합치지 않음(의도적) |
| permitAll 은 코드, 나머지는 DB | 공개 범위 변경은 배포·리뷰를 거치게 | 공개 URL 추가 시 재배포 필요 |
| 회원 본인 API 와 관리 API 의 URL 분리 (`/api/loans/{id}/return` vs `/api/loan-management/{id}/return`) | URL 인가만으로 "본인 것" 과 "전체" 를 구분 | 비슷한 API 가 두 벌 |
| 재고 감소는 조건부 UPDATE, 대출 권수 검사는 사용자 행 잠금 | 동시 대출에도 재고 음수·5권 초과가 생기지 않음 | 같은 사용자의 대출 요청이 직렬화됨 |
| 반납은 대출 행을 `PESSIMISTIC_WRITE` 로 잠근 뒤 판정 | 같은 대출을 동시에 반납(더블클릭, 회원·사서 동시 처리)해도 재고는 한 번만 늘고 두 번째는 409 `ALREADY_RETURNED` | 같은 대출의 반납 요청이 직렬화됨 |
| 도서에 낙관적 락(`version`), 재고 증감 UPDATE 도 버전을 올림 | 도서 수정이 그 사이 커밋된 대출·반납의 재고 변경을 옛 값으로 덮어쓰지 않음(충돌 시 409 `CONCURRENT_MODIFICATION`) | 수정 중 대출이 일어나면 사서가 다시 저장해야 함 |
| 토큰을 localStorage 에 보관(프론트) | 구현 단순, 새로고침 유지 | XSS 에 노출 가능. 운영이라면 Refresh 토큰은 HttpOnly·SameSite 쿠키 + CSRF 대책을 권장 |
| 관리자(ADMIN) 역할 권한 편집 금지, 자기 역할 변경 금지 | 관리자가 스스로 권한을 잃어 시스템을 못 쓰게 되는 사고 방지 | 관리자 권한 조정은 시드/DB 로만 |
| API Key 발급자는 자기 보유 액션만 부여 | 발급 권한만 가진 사용자의 권한 상승 방지 | — |
| 역할 권한 저장·액션 URL 편집·역할 변경에도 같은 원칙(`GrantGuard`) | 관리 권한 일부를 위임받은 사람이 스스로 전체 관리자가 되는 경로 차단: 자기 역할의 권한·그 액션의 URL 편집 금지(403 `CANNOT_EDIT_OWN_ROLE`, 관리자 역할의 URL 편집은 허용), 새로 부여하는 액션·역할 변경의 현재/새 역할 액션은 행위자 보유 액션 이내(403 `ACTION_NOT_OWNED`) | 위임받은 사람은 자기보다 넓은 역할의 회원을 관리할 수 없음 |
| 마지막 활성 관리자 보호 | 관리자 역할 행을 잠근 뒤 활성 관리자 수를 세어, 마지막 관리자의 역할 변경(409 `LAST_ADMIN_PROTECTED`)과 로그인 실패 자동 잠금을 막음 — 잠그면 풀 사람이 없음 | 마지막 관리자 계정은 계정 잠금 없이 IP 단위 로그인 제한만 받음 |
| 중복은 사전 검사 + 이름 붙인 유니크 제약(`uk_users_username`, `uk_books_isbn`, `uk_action_urls`) | 동시 요청이 사전 검사를 둘 다 통과해도 DB 가 막고, 전역 처리기가 이 제약 위반만 409(`DUPLICATE_USERNAME`·`DUPLICATE_ISBN`·`DUPLICATE_ACTION_URL`)로 번역 | 제약 이름을 코드가 알고 있어야 함(알 수 없는 제약 위반은 500 유지) |
| 연체 판정은 반납 예정일로 직접 계산 | 배치(`OverdueScheduler`)가 늦어도 대출 제한이 정확 | status 컬럼은 목록 필터·통계용 |

## 7. 오류 응답

```json
{ "code": "ACCESS_CONDITION_DENIED", "message": "허용된 접속 조건이 아닙니다.", "path": "/api/books",
  "timestamp": "2026-01-01T00:00:00Z", "fieldErrors": [] }
```

클라이언트는 `message` 가 아니라 `code` 로 분기한다. 401 코드: `UNAUTHORIZED`, `TOKEN_EXPIRED`, `INVALID_TOKEN`,
`INVALID_CREDENTIALS`, `ACCOUNT_LOCKED`, `INVALID_REFRESH_TOKEN`, `INVALID_API_KEY` / 403 코드: `ACCESS_DENIED`, `ACCESS_CONDITION_DENIED`
/ 429 코드: `TOO_MANY_REQUESTS`(API Key 인증 실패 한도 초과, `Retry-After` 헤더 포함).

## 8. 프론트엔드

- axios 인터셉터: 401 이면 Refresh 토큰으로 재발급 후 원 요청을 1회 재시도한다. **동시에 여러 요청이 401 을 받아도 재발급은 한 번만** 한다
  (진행 중인 재발급 Promise 공유). 재발급이 실패하면 토큰을 지우고 로그인 화면으로 보낸다.
- 403 이면 권한이 바뀌었을 수 있으므로 `/api/me/permissions` 를 다시 받고 안내한다.
- 버튼은 `<Can url="METHOD /pattern">`, 사이드바는 READ 보유 메뉴로 제어한다(자세한 근거는 [authorization-model.md](authorization-model.md)).
