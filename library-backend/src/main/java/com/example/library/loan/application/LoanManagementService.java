package com.example.library.loan.application;

import com.example.library.common.api.PageResponse;
import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import com.example.library.loan.api.CheckoutRequest;
import com.example.library.loan.api.LoanResponse;
import com.example.library.loan.domain.LoanRepository;
import com.example.library.loan.domain.LoanStatus;
import com.example.library.user.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사서의 대출 관리(현황 조회·대출 처리·반납 처리).
 */
@Service
@RequiredArgsConstructor
public class LoanManagementService {

	private final LoanRepository loanRepository;
	private final UserRepository userRepository;
	private final LoanService loanService;
	private final Clock clock;

	@Transactional(readOnly = true)
	public PageResponse<LoanResponse> search(String status, String keyword, int page, int size) {
		String normalizedStatus = normalizeStatus(status);
		LocalDate today = LocalDate.now(clock);
		PageRequest pageable = PageRequest.of(Math.max(page, 0), PageResponse.clampSize(size),
				Sort.by(Sort.Direction.DESC, "id"));
		return PageResponse.of(loanRepository.search(normalizedStatus, keyword.trim(), pageable),
				loan -> LoanResponse.of(loan, today));
	}

	@Transactional
	public LoanResponse checkout(CheckoutRequest request) {
		Long userId = userRepository.findByUsername(request.username())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
				.getId();
		return loanService.borrow(userId, request.bookId());
	}

	@Transactional
	public LoanResponse processReturn(Long loanId) {
		return loanService.returnAny(loanId);
	}

	/** 허용된 상태 값만 통과시키고, 그 외(빈 값 포함)는 전체 조회로 본다 */
	private String normalizeStatus(String status) {
		String upper = status.trim().toUpperCase(Locale.ROOT);
		boolean known = Arrays.stream(LoanStatus.values()).anyMatch(value -> value.name().equals(upper));
		return known ? upper : "";
	}
}
