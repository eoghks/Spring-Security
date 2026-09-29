package com.example.library.user.domain;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	@EntityGraph(attributePaths = "role")
	@Query("select u from User u where u.id = :id")
	Optional<User> findWithRoleById(@Param("id") Long id);

	/** 사용자 단위 직렬화가 필요한 처리(대출 권수 검사 등)를 위해 행을 잠근다 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.id = :id")
	Optional<User> findForUpdate(@Param("id") Long id);

	/** 로그인 실패 횟수를 DB 에서 원자적으로 1 올린다(동시 실패가 서로의 증가를 덮어쓰지 않게) */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update User u set u.failedLoginCount = u.failedLoginCount + 1 where u.id = :id")
	int incrementFailedLoginCount(@Param("id") Long id);

	/** DB 의 실패 횟수가 한도에 도달했고 아직 잠기지 않았을 때만 잠근다. 갱신 건수 1 이면 이번 요청이 잠갔다 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update User u set u.locked = true, u.lockedAt = :now "
			+ "where u.id = :id and u.locked = false and u.failedLoginCount >= :maxAttempts")
	int lockIfLimitReached(@Param("id") Long id, @Param("maxAttempts") int maxAttempts,
			@Param("now") LocalDateTime now);

	/** 로그인 성공 시 실패 횟수만 초기화한다(잠금 등 다른 컬럼을 옛 값으로 덮어쓰지 않게 단일 컬럼 UPDATE) */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update User u set u.failedLoginCount = 0 where u.id = :id and u.failedLoginCount > 0")
	int resetFailedLoginCount(@Param("id") Long id);

	boolean existsByIdAndLockedTrue(Long id);

	/** 아이디·이름 부분 일치 검색 (빈 문자열이면 전체) */
	@EntityGraph(attributePaths = "role")
	@Query("""
			select u from User u
			where lower(u.username) like lower(concat('%', :keyword, '%'))
			   or lower(u.name) like lower(concat('%', :keyword, '%'))
			""")
	Page<User> search(@Param("keyword") String keyword, Pageable pageable);
}
