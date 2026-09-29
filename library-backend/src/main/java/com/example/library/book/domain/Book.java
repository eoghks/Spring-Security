package com.example.library.book.domain;

import com.example.library.common.error.BusinessException;
import com.example.library.common.error.ErrorCode;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 도서. 대출 가능 수량 증감은 동시성 때문에 리포지토리의 조건부 UPDATE 로 처리한다.
 * 도서 수정(엔티티 저장)이 그 사이 커밋된 재고 변경을 덮어쓰지 않도록 낙관적 락(version)을 둔다 — 충돌하면 409.
 */
@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String isbn;

	private String title;

	private String author;

	private String publisher;

	private String category;

	private int totalQuantity;

	private int availableQuantity;

	private LocalDateTime createdAt;

	/** 낙관적 락 버전. 재고 증감 UPDATE 도 이 값을 올려 수정 요청과의 충돌을 드러낸다 */
	@Version
	private long version;

	@Builder
	private Book(String isbn, String title, String author, String publisher, String category, int totalQuantity,
			LocalDateTime createdAt) {
		this.isbn = isbn;
		this.title = title;
		this.author = author;
		this.publisher = publisher;
		this.category = category;
		this.totalQuantity = totalQuantity;
		this.availableQuantity = totalQuantity;
		this.createdAt = createdAt;
	}

	/** 현재 대출 중인 권수 */
	public int loanedQuantity() {
		return totalQuantity - availableQuantity;
	}

	/** 서지 정보 수정 */
	public void updateInfo(String isbn, String title, String author, String publisher, String category) {
		this.isbn = isbn;
		this.title = title;
		this.author = author;
		this.publisher = publisher;
		this.category = category;
	}

	/** 보유 수량 변경. 대출 중인 권수보다 작게 줄일 수 없다 */
	public void changeTotalQuantity(int newTotal) {
		int loaned = loanedQuantity();
		if (newTotal < loaned) {
			throw new BusinessException(ErrorCode.INVALID_QUANTITY);
		}
		this.totalQuantity = newTotal;
		this.availableQuantity = newTotal - loaned;
	}
}
