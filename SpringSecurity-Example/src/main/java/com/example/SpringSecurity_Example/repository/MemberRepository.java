package com.example.SpringSecurity_Example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.SpringSecurity_Example.model.entity.Member;

@Repository
public interface MemberRepository extends JpaRepository<Member, Integer>{
	public Member findByLoginId(String loginId);
}
