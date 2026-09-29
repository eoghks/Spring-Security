package com.example.library.loan.application;

import com.example.library.loan.domain.LoanRepository;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 반납 예정일이 지난 대출을 OVERDUE 로 표시한다(기동 시 1회 + 매일 00:05).
 * 대출 가능 판정은 이 배치와 무관하게 반납 예정일로 직접 계산하므로, 배치는 목록 필터·통계용이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueScheduler {

	private final LoanRepository loanRepository;
	private final Clock clock;

	@EventListener(ApplicationReadyEvent.class)
	@Scheduled(cron = "0 5 0 * * *", zone = "${app.zone-id:Asia/Seoul}")
	@Transactional
	public void markOverdueLoans() {
		int updated = loanRepository.markOverdue(LocalDate.now(clock));
		if (updated > 0) {
			log.info("연체 처리: {}건", updated);
		}
	}
}
