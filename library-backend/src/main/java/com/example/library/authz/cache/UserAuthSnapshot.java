package com.example.library.authz.cache;

import com.example.library.access.domain.AccessConditionSnapshot;
import java.io.Serial;
import java.io.Serializable;

/**
 * 요청마다 캐시에서 읽는 사용자 인증 정보. 토큰에는 넣지 않는다.
 *
 * @param userId          사용자 ID
 * @param username        아이디
 * @param roleId          역할 ID
 * @param locked          잠금 여부
 * @param accessCondition 접속 조건(미설정이면 제한 없음)
 */
public record UserAuthSnapshot(Long userId, String username, Long roleId, boolean locked,
		AccessConditionSnapshot accessCondition) implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;
}
