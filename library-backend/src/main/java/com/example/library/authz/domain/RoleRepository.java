package com.example.library.authz.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

	Optional<Role> findByCode(String code);

	List<Role> findAllByOrderByIdAsc();
}
