package com.example.library.dashboard;

import com.example.library.book.domain.BookRepository;
import com.example.library.loan.domain.LoanRepository;
import com.example.library.user.domain.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대시보드 간단 통계(DASHBOARD:READ).
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

	private final BookRepository bookRepository;
	private final LoanRepository loanRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	@GetMapping("/stats")
	@Transactional(readOnly = true)
	public DashboardStats stats() {
		return new DashboardStats(
				bookRepository.count(),
				bookRepository.sumTotalQuantity(),
				bookRepository.sumAvailableQuantity(),
				loanRepository.countByReturnedDateIsNull(),
				loanRepository.countByReturnedDateIsNullAndDueDateBefore(LocalDate.now(clock)),
				userRepository.count());
	}

	/**
	 * 통계.
	 *
	 * @param titles          도서 종수
	 * @param totalCopies     전체 보유 권수
	 * @param availableCopies 대출 가능 권수
	 * @param activeLoans     미반납 대출 수
	 * @param overdueLoans    연체 대출 수
	 * @param users           회원 수
	 */
	public record DashboardStats(long titles, long totalCopies, long availableCopies, long activeLoans,
			long overdueLoans, long users) {
	}
}
