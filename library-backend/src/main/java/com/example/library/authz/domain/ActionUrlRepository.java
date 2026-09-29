package com.example.library.authz.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ActionUrlRepository extends JpaRepository<ActionUrl, Long> {

	@Query("select u from ActionUrl u join fetch u.action a join fetch a.menu order by u.id")
	List<ActionUrl> findAllWithAction();

	boolean existsByActionIdAndHttpMethodAndUrlPattern(Long actionId, String httpMethod, String urlPattern);
}
