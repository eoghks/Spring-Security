package com.example.library.authz.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.library.authz.domain.RoleRepository;
import com.example.library.support.IntegrationTestSupport;
import com.example.library.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthzCacheIntegrationTest extends IntegrationTestSupport {

	@Autowired
	private AuthzCache authzCache;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Test
	@DisplayName("캐시 미스면 DB 에서 사용자 스냅샷을 읽어 채운다")
	void loadUserOnMiss() {
		Long memberId = userRepository.findByUsername("member").orElseThrow().getId();
		authzCache.evictUser(memberId);

		UserAuthSnapshot snapshot = authzCache.findUser(memberId).orElseThrow();

		assertThat(snapshot.username()).isEqualTo("member");
		assertThat(authzCache.findUser(memberId)).contains(snapshot);
	}

	@Test
	@DisplayName("존재하지 않는 사용자는 빈 값을 돌려준다")
	void unknownUser() {
		assertThat(authzCache.findUser(-1L)).isEmpty();
	}

	@Test
	@DisplayName("역할 보유 액션 코드는 메뉴코드:액션코드 형식이다")
	void roleActionCodes() {
		Long memberRoleId = roleRepository.findByCode("MEMBER").orElseThrow().getId();

		assertThat(authzCache.roleActionCodes(memberRoleId))
				.containsExactlyInAnyOrder("BOOK:READ", "BOOK:BORROW", "MY_LOAN:READ", "MY_LOAN:RETURN");
	}
}
