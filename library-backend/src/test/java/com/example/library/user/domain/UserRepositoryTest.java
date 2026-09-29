package com.example.library.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.authz.domain.RoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Test
	@DisplayName("시드된 샘플 계정은 역할과 함께 조회된다")
	void seededUsersHaveRoles() {
		User admin = userRepository.findByUsername("admin").orElseThrow();

		User loaded = userRepository.findWithRoleById(admin.getId()).orElseThrow();

		assertThat(loaded.getRole().getCode()).isEqualTo("ADMIN");
		assertThat(loaded.isLocked()).isFalse();
	}

	@Test
	@DisplayName("ADMIN 역할은 모든 액션을 보유한다")
	void adminRoleHasActions() {
		assertThat(roleRepository.findByCode("ADMIN").orElseThrow().getActionIds()).isNotEmpty();
	}

	@Test
	@DisplayName("키워드 검색은 아이디·이름 부분 일치로 동작한다")
	void searchByKeyword() {
		assertThat(userRepository.search("lib", PageRequest.of(0, 10)).getContent())
				.extracting(User::getUsername)
				.containsExactly("librarian");
	}
}
