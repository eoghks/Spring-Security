package com.example.library.apikey.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

	Optional<ApiKey> findByKeyHash(String keyHash);

	List<ApiKey> findAllByOrderByIdDesc();

	/** 마지막 사용 시각 갱신(엔티티를 읽지 않는 단건 UPDATE) */
	@Transactional
	@Modifying
	@Query("update ApiKey k set k.lastUsedAt = :now where k.id = :id")
	int touchLastUsed(@Param("id") Long id, @Param("now") LocalDateTime now);
}
