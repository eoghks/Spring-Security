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
| 재사용 탐지 | 이미 폐기된 Refresh 토큰이 오면 탈취로 보고 그 사용자의 토큰을 모두 폐기 |
| 로그아웃 | 제시된 Refresh 토큰 폐기(멱등) |
| 비밀번호 | BCrypt |
| 잠금 | 연속 5회 실패 시 잠금, 관리자만 해제. 잠기면 기존 Access 토큰도 다음 요청부터 401 `ACCOUNT_LOCKED` |

- 없는 아이디도 더미 해시와 비교해 응답 시간으로 계정 존재 여부가 드러나지 않게 했다.
- 실패 횟수는 로그인 트랜잭션과 분리해 로그인이 예외로 끝나도 커밋된다.

### 권한을 토큰에 넣지 않는 이유

JWT 에 역할·권한을 넣으면 관리자가 권한을 회수해도 토큰 만료(15분)까지 옛 권한이 살아 있다.
토큰에는 userId 만 넣고, **매 요청 캐시에서** 역할·잠금·접속 조건을 읽는다. 캐시는 변경 커밋 직후 해당 키만 evict 되므로
권한 회수·잠금·접속 조건 변경이 **다음 요청부터 즉시** 적용된다. 대가는 요청마다 캐시 조회 1~2회다.

## 4. 캐시와 evict

| IMap | 키 → 값 | evict 시점 |
|---|---|---|
| `user-auth` | userId → 역할 ID·잠금·접속 조건 | 역할 변경, 잠금/해제, 접속 조건 저장·삭제 |
| `role-actions` | roleId → 보유 액션 코드 집합 | 역할 권한 저장 |
| `api-keys` | 키 해시 → 부여 액션·허용 IP·만료·폐기 | API Key 폐기 |

- **캐시 미스면 DB 에서 읽어 채운다. 미스를 통과로 취급하지 않는다.**
- evict 는 `@TransactionalEventListener(AFTER_COMMIT)` 에서만 한다. 커밋 전에 지우면 다른 요청이 옛 DB 값을 다시 적재할 수 있다.
- 값 객체는 `Serializable`(serialVersionUID 명시). `Optional` 은 직렬화되지 않으므로 필드는 nullable 로 두고 접근자에서 `Optional` 로 감싼다.
- 안전장치: 모든 IMap 에 TTL 600초. evict 가 어떤 이유로 누락돼도 오래된 권한이 무기한 남지 않는다.
- 알려진 경합: "캐시 미스 → DB 읽기" 와 "evict" 가 동시에 일어나면 옛 값이 다시 들어갈 수 있다(짧은 창). TTL 이 상한을 보장한다.

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

설정: 멀티캐스트는 끄고 TCP/IP 멤버 목록(`app.hazelcast.members`, 기본 `127.0.0.1`)으로 합류한다. 단일 노드로 그대로 뜬다.

```bash
# 같은 PC 에서 2노드 예시(두 번째 노드는 포트만 다르게)
java -jar library-backend.jar --server.port=8080
java -jar library-backend.jar --server.port=8081   # Hazelcast 포트는 5701 → 5702 자동 증가 후 합류
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
| 토큰을 localStorage 에 보관(프론트) | 구현 단순, 새로고침 유지 | XSS 에 노출 가능. 운영이라면 Refresh 토큰은 HttpOnly·SameSite 쿠키 + CSRF 대책을 권장 |
| 관리자(ADMIN) 역할 권한 편집 금지, 자기 역할 변경 금지 | 관리자가 스스로 권한을 잃어 시스템을 못 쓰게 되는 사고 방지 | 관리자 권한 조정은 시드/DB 로만 |
| API Key 발급자는 자기 보유 액션만 부여 | 발급 권한만 가진 사용자의 권한 상승 방지 | — |
| 연체 판정은 반납 예정일로 직접 계산 | 배치(`OverdueScheduler`)가 늦어도 대출 제한이 정확 | status 컬럼은 목록 필터·통계용 |

## 7. 오류 응답

```json
{ "code": "ACCESS_CONDITION_DENIED", "message": "허용된 접속 조건이 아닙니다.", "path": "/api/books",
  "timestamp": "2026-01-01T00:00:00Z", "fieldErrors": [] }
```

클라이언트는 `message` 가 아니라 `code` 로 분기한다. 401 코드: `UNAUTHORIZED`, `TOKEN_EXPIRED`, `INVALID_TOKEN`,
`INVALID_CREDENTIALS`, `ACCOUNT_LOCKED`, `INVALID_REFRESH_TOKEN`, `INVALID_API_KEY` / 403 코드: `ACCESS_DENIED`, `ACCESS_CONDITION_DENIED`.

## 8. 프론트엔드

- axios 인터셉터: 401 이면 Refresh 토큰으로 재발급 후 원 요청을 1회 재시도한다. **동시에 여러 요청이 401 을 받아도 재발급은 한 번만** 한다
  (진행 중인 재발급 Promise 공유). 재발급이 실패하면 토큰을 지우고 로그인 화면으로 보낸다.
- 403 이면 권한이 바뀌었을 수 있으므로 `/api/me/permissions` 를 다시 받고 안내한다.
- 버튼은 `<Can url="METHOD /pattern">`, 사이드바는 READ 보유 메뉴로 제어한다(자세한 근거는 [authorization-model.md](authorization-model.md)).
