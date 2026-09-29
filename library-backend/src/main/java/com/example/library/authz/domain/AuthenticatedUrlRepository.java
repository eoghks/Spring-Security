package com.example.library.authz.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthenticatedUrlRepository extends JpaRepository<AuthenticatedUrl, Long> {
}
