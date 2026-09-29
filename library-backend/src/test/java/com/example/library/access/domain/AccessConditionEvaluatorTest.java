package com.example.library.access.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccessConditionEvaluatorTest {

	/** 2026-03-02 는 월요일 */
	private static final LocalDateTime MONDAY_10AM = LocalDateTime.of(2026, 3, 2, 10, 0);
	private static final String OFFICE_IP = "192.168.10.20";

	@Test
	@DisplayName("조건이 없으면 항상 통과한다")
	void unrestricted() {
		assertThat(evaluate(AccessConditionSnapshot.unrestricted(), "8.8.8.8", MONDAY_10AM)).isEmpty();
	}

	@Test
	@DisplayName("IP: 단일 IP 또는 CIDR 중 하나와 일치해야 한다")
	void ip() {
		AccessConditionSnapshot condition = AccessConditionSnapshot.builder()
				.allowedIps(List.of("10.0.0.1", "192.168.10.0/24")).build();

		assertThat(evaluate(condition, OFFICE_IP, MONDAY_10AM)).isEmpty();
		assertThat(evaluate(condition, "10.0.0.1", MONDAY_10AM)).isEmpty();
		assertThat(evaluate(condition, "172.16.0.1", MONDAY_10AM)).contains("허용되지 않은 IP");
	}

	@Test
	@DisplayName("기간: 시작일·종료일 당일은 포함한다")
	void period() {
		AccessConditionSnapshot condition = AccessConditionSnapshot.builder()
				.validFrom(LocalDate.of(2026, 3, 2)).validTo(LocalDate.of(2026, 3, 31)).build();

		assertThat(evaluate(condition, OFFICE_IP, MONDAY_10AM)).isEmpty();
		assertThat(evaluate(condition, OFFICE_IP, LocalDateTime.of(2026, 3, 31, 23, 59))).isEmpty();
		assertThat(evaluate(condition, OFFICE_IP, LocalDateTime.of(2026, 3, 1, 12, 0))).contains("허용 기간 밖");
		assertThat(evaluate(condition, OFFICE_IP, LocalDateTime.of(2026, 4, 1, 0, 0))).contains("허용 기간 밖");
	}

	@Test
	@DisplayName("요일: 허용 요일이 아니면 거부한다")
	void days() {
		AccessConditionSnapshot weekdays = AccessConditionSnapshot.builder()
				.allowedDays(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
						DayOfWeek.FRIDAY)).build();

		assertThat(evaluate(weekdays, OFFICE_IP, MONDAY_10AM)).isEmpty();
		assertThat(evaluate(weekdays, OFFICE_IP, MONDAY_10AM.minusDays(1))).contains("허용되지 않은 요일");
	}

	@Test
	@DisplayName("시간: 09:00~18:00 경계 포함")
	void timeWindow() {
		AccessConditionSnapshot office = AccessConditionSnapshot.builder()
				.startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(18, 0)).build();

		assertThat(evaluate(office, OFFICE_IP, MONDAY_10AM.withHour(9))).isEmpty();
		assertThat(evaluate(office, OFFICE_IP, MONDAY_10AM.withHour(18))).isEmpty();
		assertThat(evaluate(office, OFFICE_IP, MONDAY_10AM.withHour(8).withMinute(59))).contains("허용 시간대 밖");
		assertThat(evaluate(office, OFFICE_IP, MONDAY_10AM.withHour(18).withMinute(1))).contains("허용 시간대 밖");
	}

	@Test
	@DisplayName("시간: 시작이 종료보다 늦으면 자정을 넘는 구간으로 본다(22:00~06:00)")
	void overnightWindow() {
		AccessConditionSnapshot night = AccessConditionSnapshot.builder()
				.startTime(LocalTime.of(22, 0)).endTime(LocalTime.of(6, 0)).build();

		assertThat(evaluate(night, OFFICE_IP, MONDAY_10AM.withHour(23))).isEmpty();
		assertThat(evaluate(night, OFFICE_IP, MONDAY_10AM.withHour(5))).isEmpty();
		assertThat(evaluate(night, OFFICE_IP, MONDAY_10AM.withHour(12))).contains("허용 시간대 밖");
	}

	@Test
	@DisplayName("엔티티 CSV 값과 스냅샷이 서로 변환된다")
	void entityRoundTrip() {
		AccessConditionSnapshot original = AccessConditionSnapshot.builder()
				.allowedIps(List.of("10.0.0.0/8")).allowedDays(List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
				.startTime(LocalTime.of(9, 0)).build();
		UserAccessCondition entity = new UserAccessCondition(1L);

		entity.update(original, LocalDateTime.now());

		assertThat(entity.toSnapshot()).isEqualTo(original);
	}

	private Optional<String> evaluate(AccessConditionSnapshot condition, String ip, LocalDateTime now) {
		return AccessConditionEvaluator.findViolation(condition, ip, now);
	}
}
