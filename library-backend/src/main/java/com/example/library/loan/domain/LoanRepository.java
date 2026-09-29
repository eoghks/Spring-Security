package com.example.library.loan.domain;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

	/** 미반납 대출 권수 */
	long countByUserIdAndReturnedDateIsNull(Long userId);

	/** 연체(미반납 + 반납 예정일 경과) 대출 존재 여부 */
	boolean existsByUserIdAndReturnedDateIsNullAndDueDateBefore(Long userId, LocalDate today);

	boolean existsByUserIdAndBookIdAndReturnedDateIsNull(Long userId, Long bookId);

	boolean existsByBookId(Long bookId);

	/** 반납 처리용 조회. 대출 행을 잠가 같은 대출의 동시 반납을 직렬화한다(두 번째 요청은 반납된 상태를 읽는다) */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select l from Loan l where l.id = :id")
	Optional<Loan> findForUpdate(@Param("id") Long id);

	@EntityGraph(attributePaths = {"book", "user"})
	List<Loan> findByUserIdOrderByIdDesc(Long userId);

	/** 대출 관리 검색: 상태(빈 문자열이면 전체) + 아이디·도서 제목 부분 일치(keyword 는 LikePatterns.escape 로 이스케이프) */
	@EntityGraph(attributePaths = {"book", "user"})
	@Query("""
			select l from Loan l join l.user u join l.book b
			where (:status = '' or cast(l.status as string) = :status)
			  and (lower(u.username) like lower(concat('%', :keyword, '%')) escape '\\'
			    or lower(b.title) like lower(concat('%', :keyword, '%')) escape '\\')
			""")
	Page<Loan> search(@Param("status") String status, @Param("keyword") String keyword, Pageable pageable);

	/** 반납 예정일이 지난 대출을 OVERDUE 로 표시한다 */
	@Modifying
	@Query("update Loan l set l.status = com.example.library.loan.domain.LoanStatus.OVERDUE "
			+ "where l.status = com.example.library.loan.domain.LoanStatus.LOANED and l.dueDate < :today")
	int markOverdue(@Param("today") LocalDate today);

	long countByReturnedDateIsNull();

	long countByReturnedDateIsNullAndDueDateBefore(LocalDate today);
}
