# 도서관 대출 시스템 — Spring Security URL 기반 인가 예제

Spring Security 6 로 **메뉴 · 액션 · URL** 모델의 동적 인가를 구현한 도서관 대출 시스템이다.
권한은 DB 로 운영하고, 토큰에는 사용자 ID 만 담아 **권한 회수가 다음 요청부터 즉시** 반영되도록 설계했다.

> 기존 학습 예제(Spring Security + JWT 회원가입·로그인 실습)는 [`SpringSecurity-Example/`](SpringSecurity-Example/) 에 그대로 있다.
> 그 예제의 원래 README 는 이 문서 맨 아래 "이전 학습 예제" 에 보존했다.

## 주요 기능

- **URL 기반 인가** — 커스텀 `AuthorizationManager` 하나가 `action_urls` 를 `PathPattern` 으로 판정.
  구체적인 패턴 우선, 같은 URL 은 여러 액션 중 하나만 있어도 통과(OR), **미등록 URL 은 403(fail-closed)**
- **JWT + Refresh 토큰 회전** — Access 15분(subject=userId 만), Refresh 7일(DB 에 해시만, 재사용 탐지)
- **로그인 5회 실패 잠금**(잠금 여부는 비밀번호가 맞을 때만 응답), 관리자 해제, IP 별 로그인 실패 1분 30회 초과 시 429
- **분산 권한 캐시(Hazelcast embedded)** — 역할 권한·사용자 역할·접속 조건·API Key 를 캐시하고 변경 커밋 후 해당 키만 evict
- **사용자 접속 조건** — IP/CIDR, 기간, 요일, 시간대(자정 넘는 구간 포함), 신뢰 프록시 기반 클라이언트 IP
- **API Key** — `X-API-KEY` 헤더, 사용자와 같은 액션 체계로 권한 부여, 원문 1회 표시·해시 저장, 허용 IP,
  없는 키 음성 캐시(60초)·IP 별 인증 실패 1분 20회 초과 시 429 ([설계](docs/architecture.md#api-key-무차별-대입db-부하-방어))
- **도서관 도메인** — 도서 검색·관리, 대출(1인 5권·연체 시 불가·14일)·반납, 사서 대출 처리, 대시보드
- **프론트 권한 표시** — `<Can url="POST /api/loans">` 로 버튼 제어, READ 보유 메뉴로 사이드바 구성
- **누락 방지** — 컨트롤러 매핑 ↔ 인가 규칙 대조 테스트, 프론트 `<Can url>` ↔ 시드 대조 스크립트

## 기술 스택

| 영역 | 스택 |
|---|---|
| 백엔드 | Java 21, Spring Boot 3.5, Spring Security 6.5, Spring Data JPA, jjwt 0.12, Hazelcast 5.5, springdoc-openapi, Lombok, Gradle |
| DB | H2 인메모리(기본, PostgreSQL 호환 모드) / PostgreSQL(`postgres` 프로필) — `schema.sql` + `data.sql` |
| 프론트 | React 18, TypeScript, Vite, react-router, axios, 순수 CSS |

## 실행 방법

### 백엔드 (포트 8080)

JDK 21 이 필요하다(Gradle toolchain 이 21 을 찾는다).

```bash
cd library-backend
./gradlew bootRun            # Windows: gradlew.bat bootRun
```

- 기본 프로필은 H2 인메모리라 설치 없이 뜨며, 기동할 때마다 시드가 다시 적재된다.
- JWT 서명 키: 기본(H2) 개발 프로필에서만 저장소에 있는 개발용 키를 쓴다(`application-default.yml`).
  그 밖의 프로필(`postgres` 등)은 환경 변수 `JWT_SECRET`(Base64, 디코드 32바이트 이상)이 **필수**이며,
  없거나 개발용 키와 같거나 짧으면 **기동에 실패**한다. 키 생성 예: `openssl rand -base64 32`
- Swagger UI: http://localhost:8080/swagger-ui.html
- 빌드·테스트: `./gradlew build`

PostgreSQL 로 실행 (Docker 전용 컨테이너 — 로컬에 설치된 5432 PostgreSQL 과 겹치지 않게 호스트 포트 **55432**):

```bash
cd library-backend
docker compose up -d                       # postgres:16, db/user/password = library (개발용)

export JWT_SECRET="$(openssl rand -base64 32)"   # postgres 프로필은 JWT_SECRET 필수(없으면 기동 실패)

# 최초 1회는 스키마·시드 적용
DB_INIT_MODE=always ./gradlew bootRun --args='--spring.profiles.active=postgres'
# 이후에는 DB_INIT_MODE 없이 실행(데이터 유지)
./gradlew bootRun --args='--spring.profiles.active=postgres'

docker compose down -v                     # 정리(볼륨까지 삭제)
```

- `postgres` 프로필 기본 접속은 `jdbc:postgresql://localhost:55432/library` 이고 `DB_URL`·`DB_USERNAME`·`DB_PASSWORD` 로 덮어쓴다.
- 같은 `schema.sql`·`data.sql` 이 H2(PostgreSQL 모드)와 PostgreSQL 16 양쪽에서 수정 없이 적재된다. 수동 검증 항목: 시드 적재,
  로그인·도서 검색/상세·대출/반납·권한 403·API Key 조회·없는 키 429·동시 가입 409·접속 조건 밖 로그인 403, 재기동 후 데이터 유지.
  빌드가 Docker 에 의존하지 않도록 PostgreSQL 자동 테스트(Testcontainers)는 두지 않았다.

주요 환경 변수: `JWT_SECRET`(Base64·32바이트 이상, 기본 개발 프로필 외 필수), `TRUSTED_PROXIES`(프록시 자신의 주소만, 예: `10.0.0.5/32` — 클라이언트가 속한 넓은 대역을 넣지 않는다),
`HAZELCAST_MEMBERS`(예: `10.0.0.11,10.0.0.12`), `HAZELCAST_INTERFACE`(기본 `127.0.0.1`, 다중 노드는 사설 대역 예: `10.0.0.*`),
`HAZELCAST_PORT`, `HAZELCAST_CLUSTER`,
`API_KEY_NEGATIVE_CACHE_TTL`(기본 `60s`), `API_KEY_MAX_FAILURES_PER_MINUTE`(기본 `20`), `LOGIN_MAX_FAILURES_PER_MINUTE`(기본 `30`).

### 프론트엔드 (포트 5173)

```bash
cd library-frontend
npm install
npm run dev                  # http://localhost:5173 — /api 는 localhost:8080 으로 프록시
npm run build                # <Can url> 시드 대조 → 타입체크 → 번들
```

## 샘플 계정 (개발용)

| 아이디 | 비밀번호 | 역할 | 볼 수 있는 메뉴 |
|---|---|---|---|
| `admin` | `Admin123!` | 관리자 | 전체 |
| `librarian` | `Librarian123!` | 사서 | 대시보드, 대출 관리, 도서 관리 |
| `member` | `Member123!` | 일반 회원 | 도서 목록, 내 대출 |

샘플 API Key(외부 도서 조회, `BOOK:READ`): `lib_Elsu1z3_KkwNJYk7v7R7BxLaFBJpJV4qc61GHDjZvNY`

```bash
curl -H "X-API-KEY: lib_Elsu1z3_KkwNJYk7v7R7BxLaFBJpJV4qc61GHDjZvNY" "http://localhost:8080/api/books?keyword=클린"
```

> 위 비밀번호·키는 **개발용**이다. 운영에서는 반드시 교체한다. 개발용 JWT 키는 기본(H2) 개발 프로필에서만 허용된다.

## 스크린샷

| 화면 | 이미지 |
|---|---|
| 로그인 | _(추가 예정)_ |
| 도서 목록 | _(추가 예정)_ |
| 역할·권한 관리 | _(추가 예정)_ |
| 접속 조건 관리 | _(추가 예정)_ |
| API Key 발급 | _(추가 예정)_ |

## 문서

- [아키텍처 — 필터 체인, 판정 규칙, 캐시·evict, 설계 트레이드오프](docs/architecture.md)
- [인가 모델 — 메뉴/액션/URL, OR 규칙, fail-closed, API Key, 접속 조건](docs/authorization-model.md)
- [API 명세](docs/api.md)
- [ERD](docs/erd.md)

## 디렉터리

```
library-backend/          Spring Boot 백엔드
library-frontend/         React 프론트엔드
docs/                     설계 문서
SpringSecurity-Example/   이전 학습 예제(보존)
```

---

## 이전 학습 예제 (SpringSecurity-Example)

<details>
<summary>원래 README 펼치기</summary>

$\bf \large 요구\ 사항$

1. Spring Security와 JWT를 이용한 회원가입 및 로그인 구현

---

$\bf \large 제약\ 사항$

1. memberId가 1인 경우 해당 유저는 admin 권한을 가진다. 나머지 사용자는 user 권한을 가진다.

---

$\bf \large 개발\ 환경$

- Eclipse 2022-12R
- Java 17
- Spring Boot 3.3.0

---

$\bf \large 기술\ 스택$

- Spring Boot
    - Spring Web
    - Spring Boot DevTools
    - Spring JPA
- Spring Security
- PostgreSQL
- JWT

---
$\bf \large 개발\ 기간\ 및\ 진행\ 과정$

- 1차 개발
    - 개발 기간:  2024-06-06 ~ 2024-06-17

    ![image](https://github.com/eoghks/Spring-Security/assets/62344247/5be5b73f-c12d-4cde-af8f-15c332a9a2da)

- 2차 개발
    - 개발 예정
    - 추가 예정 기능
        - 추후 RefreshToken을 이용한 JWT Token 재발급 기능을 추가한다. (Redis 사용 예정<연구 필요!!>)

---

$\bf \large Database\ 명세$

- Member

    | Column | Type | 설명 | 비고 |
    | --- | --- | --- | --- |
    | memberId | Long | 사용자 Id | primary key, Auto Increment |
    | loginId | VarChar(64) | 로그인 Id | unique, not null |
    | password | VarChar(64) | 로그인 Id | not null |
- crtab

    ```sql
    create user security_example password 'security_example' SUPERUSER;

    create database security_example with owner security_example encoding 'UTF8';

    create sequence member_id_seq;

    //생성한 사용자로 로그인 후 테이블 생성이 필요합니다.
    create table member(
    	memberId int8 primary key default nextval('member_id_seq'),
    	loginId  Varchar(64) not null unique,
    	password Varchar(64) not null
    );
    ```

---
$\bf \large API 명세$
![image](https://github.com/eoghks/Spring-Security/assets/62344247/5c109c52-64e2-431a-bce1-c6e027d8cdd1)
(자세한 API 명세서는 하단의 Notion을 참조하십시오.)

---
$\bf \large Notion$
https://knowing-parakeet-f9a.notion.site/eaff7432befc447b85348280e15cad2c?pvs=4

</details>
