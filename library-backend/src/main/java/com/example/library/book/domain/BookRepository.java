package com.example.library.book.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

	/** 제목·저자·ISBN 부분 일치 + 분류 일치 검색(빈 문자열이면 조건 없음) */
	@Query("""
			select b from Book b
			where (lower(b.title) like lower(concat('%', :keyword, '%'))
			    or lower(b.author) like lower(concat('%', :keyword, '%'))
			    or b.isbn like concat('%', :keyword, '%'))
			  and (:category = '' or b.category = :category)
			""")
	Page<Book> search(@Param("keyword") String keyword, @Param("category") String category, Pageable pageable);

	@Query("select distinct b.category from Book b order by b.category")
	List<String> findCategories();

	@Query("select coalesce(sum(b.totalQuantity), 0) from Book b")
	long sumTotalQuantity();

	@Query("select coalesce(sum(b.availableQuantity), 0) from Book b")
	long sumAvailableQuantity();

	boolean existsByIsbn(String isbn);

	boolean existsByIsbnAndIdNot(String isbn, Long id);

	/**
	 * 재고가 있을 때만 1 감소(동시 대출 경합 방지). 갱신 건수 0 이면 재고 없음.
	 * 버전도 올려, 이 도서를 읽어 둔 수정 요청이 옛 재고로 덮어쓰지 못하고 낙관적 락 충돌이 나게 한다.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update Book b set b.availableQuantity = b.availableQuantity - 1, b.version = b.version + 1 "
			+ "where b.id = :id and b.availableQuantity > 0")
	int decrementAvailable(@Param("id") Long id);

	/** 반납 시 1 증가(보유 수량을 넘지 않게). 감소와 같은 이유로 버전도 올린다 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update Book b set b.availableQuantity = b.availableQuantity + 1, b.version = b.version + 1 "
			+ "where b.id = :id and b.availableQuantity < b.totalQuantity")
	int incrementAvailable(@Param("id") Long id);
}
