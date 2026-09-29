package com.example.library.user.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	@EntityGraph(attributePaths = "role")
	@Query("select u from User u where u.id = :id")
	Optional<User> findWithRoleById(@Param("id") Long id);

	/** 아이디·이름 부분 일치 검색 (빈 문자열이면 전체) */
	@EntityGraph(attributePaths = "role")
	@Query("""
			select u from User u
			where lower(u.username) like lower(concat('%', :keyword, '%'))
			   or lower(u.name) like lower(concat('%', :keyword, '%'))
			""")
	Page<User> search(@Param("keyword") String keyword, Pageable pageable);
}
