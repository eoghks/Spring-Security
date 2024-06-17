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

$\bf \large Database\ 명세$

- Member
    
    
    | Column | Type | 설명 | 비고 |
    | --- | --- | --- | --- |
    | memberId | Long | 사용자 Id | primary key,
    Auto Increment |
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

---

$\bf \large 프로젝트\ 진행\ 과정$

---

$\bf \large 코드$

https://github.com/eoghks/Spring-Security

---

**$\bf \large 보완점$**

추후 RefreshToken을 이용한 JWT Token 재발급 기능을 추가한다.
---

자세한 정보는 https://knowing-parakeet-f9a.notion.site/eaff7432befc447b85348280e15cad2c?pvs=4 참조

