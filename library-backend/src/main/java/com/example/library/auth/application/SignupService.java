package com.example.library.auth.application;

import com.example.library.auth.api.SignupRequest;
import com.example.library.auth.api.SignupResponse;
import com.example.library.authz.domain.Role;
import com.example.library.authz.domain.RoleRepository;
import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import com.example.library.user.domain.User;
import com.example.library.user.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입. 일반 회원(MEMBER) 역할을 자동 부여한다.
 */
@Service
@RequiredArgsConstructor
public class SignupService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	@Transactional
	public SignupResponse signup(SignupRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
		}
		Role memberRole = roleRepository.findByCode(Role.MEMBER_CODE)
				.orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
		User user = User.builder()
				.username(request.username())
				.password(passwordEncoder.encode(request.password()))
				.name(request.name())
				.email(request.email())
				.role(memberRole)
				.createdAt(LocalDateTime.now(clock))
				.build();
		User saved = userRepository.save(user);
		return new SignupResponse(saved.getId(), saved.getUsername());
	}
}
