package com.example.library.authz.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {

	List<Menu> findAllByOrderBySortOrderAsc();
}
