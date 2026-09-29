# ERD

스키마 원본은 [`library-backend/src/main/resources/schema.sql`](../library-backend/src/main/resources/schema.sql) 이다.
JPA `ddl-auto` 는 `validate` 로 두어 엔티티 매핑과 스키마가 어긋나면 기동이 실패한다.

```mermaid
erDiagram
    roles ||--o{ users : "역할 1개"
    roles ||--o{ role_actions : ""
    menus ||--o{ menu_actions : ""
    menu_actions ||--o{ action_urls : "URL 여러 개"
    menu_actions ||--o{ role_actions : ""
    menu_actions ||--o{ api_key_actions : ""
    users ||--o{ refresh_tokens : ""
    users ||--o| user_access_conditions : "0..1"
    users ||--o{ api_keys : "발급자"
    api_keys ||--o{ api_key_actions : ""
    api_keys ||--o{ api_key_allowed_ips : ""
    users ||--o{ loans : ""
    books ||--o{ loans : ""

    roles {
        BIGINT id PK
        VARCHAR code UK "ADMIN / LIBRARIAN / MEMBER"
        VARCHAR name
    }
    users {
        BIGINT id PK
        VARCHAR username UK "uk_users_username"
        VARCHAR password "BCrypt"
        VARCHAR name
        VARCHAR email
        BIGINT role_id FK
        INT failed_login_count
        BOOLEAN locked
        TIMESTAMP locked_at
        TIMESTAMP created_at
    }
    refresh_tokens {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR token_hash UK "SHA-256 hex"
        TIMESTAMP expires_at
        TIMESTAMP revoked_at
        TIMESTAMP created_at
    }
    menus {
        BIGINT id PK
        VARCHAR code UK
        VARCHAR name
        VARCHAR path
        INT sort_order
    }
    menu_actions {
        BIGINT id PK
        BIGINT menu_id FK
        VARCHAR code "메뉴 안에서 유일"
        VARCHAR name
        VARCHAR action_type "READ / ACTION"
    }
    action_urls {
        BIGINT id PK
        BIGINT action_id FK
        VARCHAR http_method
        VARCHAR url_pattern "PathPattern"
    }
    role_actions {
        BIGINT role_id PK,FK
        BIGINT action_id PK,FK
    }
    authenticated_urls {
        BIGINT id PK
        VARCHAR http_method
        VARCHAR url_pattern
        VARCHAR description
    }
    user_access_conditions {
        BIGINT user_id PK,FK
        VARCHAR allowed_ips "콤마 구분 IP/CIDR"
        DATE valid_from
        DATE valid_to
        VARCHAR allowed_days "MON,TUE,..."
        TIME start_time
        TIME end_time
        TIMESTAMP updated_at
    }
    api_keys {
        BIGINT id PK
        VARCHAR name
        VARCHAR key_prefix "앞 8자"
        VARCHAR key_hash UK "SHA-256 hex"
        BIGINT owner_user_id FK
        TIMESTAMP expires_at
        TIMESTAMP revoked_at
        TIMESTAMP created_at
        TIMESTAMP last_used_at
    }
    api_key_actions {
        BIGINT api_key_id PK,FK
        BIGINT action_id PK,FK
    }
    api_key_allowed_ips {
        BIGINT api_key_id PK,FK
        VARCHAR ip PK
    }
    books {
        BIGINT id PK
        VARCHAR isbn UK
        VARCHAR title
        VARCHAR author
        VARCHAR publisher
        VARCHAR category
        INT total_quantity
        INT available_quantity "0 이상, total 이하"
        TIMESTAMP created_at
    }
    loans {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT book_id FK
        DATE loan_date
        DATE due_date "loan_date + 14"
        DATE returned_date
        VARCHAR status "LOANED / RETURNED / OVERDUE"
    }
```

## 메모

- `menu_actions.code` 는 메뉴 안에서만 유일하다. 전역 식별자는 `메뉴코드:액션코드`(예: `BOOK_MANAGE:CREATE`)다.
- `action_urls` 는 `(action_id, http_method, url_pattern)` 가 유일하다. 같은 (메서드, 패턴)이 여러 액션에 있을 수 있고, 판정은 OR 이다.
- `user_access_conditions.allowed_ips` 는 명세상 TEXT 지만 H2(PostgreSQL 모드)와 PostgreSQL 양쪽에서 같은 타입으로 매핑되도록 `VARCHAR(2000)` 으로 두었다.
- `public_urls` 테이블은 두지 않는다. permitAll 목록은 코드(`PublicEndpoints`) 한 곳에서만 관리한다.
