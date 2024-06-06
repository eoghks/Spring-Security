package com.example.SpringSecurity_Example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.SpringSecurity_Example.model.Users;

@Repository
public interface UserRepository extends JpaRepository<Users, Integer>{

}
