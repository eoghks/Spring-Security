# Spring-Security
$\bf \large 요구\ 사항$

1. Spring Security와 JWT를 이용한 회원가입 및 로그인 구현

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

- Users
    
    
    | Column | Type | 설명 | 비고 |
    | --- | --- | --- | --- |
    | id | Long | 사용자 Id | primary key |
    | password | varChar(64) | 로그인 Id | unique, not null |
- crtab
    
    ```sql
    create user security_example password 'security_example' SUPERUSER;
    
    create database security_example with owner security_example encoding 'UTF8';
    
    create table users(
    	user_id int8 primary key,
    	password Varchar(256) not null
    );
    
    ```
    

---

자세한 정보는 https://knowing-parakeet-f9a.notion.site/eaff7432befc447b85348280e15cad2c?pvs=4 참조

