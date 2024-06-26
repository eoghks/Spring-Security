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
![image](https://github.com/eoghks/Spring-Security/assets/62344247/5c109c52-64e2-431a-bce1-c6e027d8cdd1)
(자세한 API 명세서는 하단의 Notion을 참조하십시오.)
  
---
$\bf \large Notion$  
https://knowing-parakeet-f9a.notion.site/eaff7432befc447b85348280e15cad2c?pvs=4

