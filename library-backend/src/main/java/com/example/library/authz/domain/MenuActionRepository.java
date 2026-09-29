package com.example.library.authz.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuActionRepository extends JpaRepository<MenuAction, Long> {

	@Query("select a from MenuAction a join fetch a.menu m order by m.sortOrder, a.id")
	List<MenuAction> findAllWithMenu();

	@Query("select a from MenuAction a join fetch a.menu where a.id in :ids")
	List<MenuAction> findAllWithMenuByIdIn(@Param("ids") Collection<Long> ids);
}
