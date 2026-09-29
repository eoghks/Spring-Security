package com.example.library.auth.domain;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/**
	 * 아직 폐기되지 않은 토큰만 폐기한다(원자적 소비). 갱신 건수가 1 이면 이 요청이 소비에 성공한 것이고,
	 * 0 이면 같은 토큰을 동시에 제시한 다른 요청이 먼저 소비한 것이다.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update RefreshToken t set t.revokedAt = :now where t.id = :id and t.revokedAt is null")
	int revokeIfActive(@Param("id") Long id, @Param("now") LocalDateTime now);

	/** 사용자의 살아 있는 토큰을 모두 폐기한다(재사용 탐지 시, 로그인 실패로 잠길 때) */
	@Modifying
	@Query("update RefreshToken t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
	int revokeAllByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
