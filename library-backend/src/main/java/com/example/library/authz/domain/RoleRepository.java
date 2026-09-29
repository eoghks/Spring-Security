package com.example.library.authz.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, Long> {

	Optional<Role> findByCode(String code);

	List<Role> findAllByOrderByIdAsc();

	/** 역할 행을 잠근다(마지막 관리자 판정을 직렬화할 때 관리자 역할 행을 잠금 대상으로 쓴다) */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Role r where r.id = :id")
	Optional<Role> findForUpdate(@Param("id") Long id);
}
