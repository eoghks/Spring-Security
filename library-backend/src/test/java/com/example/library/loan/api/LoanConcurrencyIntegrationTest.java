package com.example.library.loan.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.book.api.BookRequest;
import com.example.library.book.application.BookCommandService;
import com.example.library.book.domain.BookRepository;
import com.example.library.support.Concurrently;
import com.example.library.support.IntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * 대출·반납 동시 요청에서 재고가 두 번 늘거나 음수가 되지 않는지 검증한다.
 */
class LoanConcurrencyIntegrationTest extends IntegrationTestSupport {

	private static final String PASSWORD = "Passw0rd!";
	private static final int ROUNDS = 5;

	@Autowired
	private BookCommandService bookCommandService;

	@Autowired
	private BookRepository bookRepository;

	@Test
	@DisplayName("같은 대출을 회원과 사서가 동시에 반납하면 한쪽만 200, 다른 쪽은 409 이고 재고는 1만 늘어난다")
	void concurrentReturnOfSameLoan() throws Exception {
		String librarian = bearer("librarian", LIBRARIAN_PASSWORD);
		for (int round = 0; round < ROUNDS; round++) {
			long bookId = createBook("97800000010" + round, 3);
			String first = memberToken("retrace" + round + "a");
			String second = memberToken("retrace" + round + "b");
			long loanId = borrow(first, bookId);
			borrow(second, bookId);

			List<Integer> statuses = Concurrently.run(List.of(
					() -> returnStatus("/api/loans/" + loanId + "/return", first),
					() -> returnStatus("/api/loan-management/" + loanId + "/return", librarian)));

			assertThat(statuses).containsExactlyInAnyOrder(200, 409);
			assertThat(availableQuantity(bookId)).isEqualTo(2);
		}
	}

	@Test
	@DisplayName("마지막 1권을 두 회원이 동시에 대출하면 한쪽만 201, 다른 쪽은 409 이고 재고는 0 에서 멈춘다")
	void concurrentBorrowOfLastCopy() throws Exception {
		for (int round = 0; round < ROUNDS; round++) {
			long bookId = createBook("97800000020" + round, 1);
			String first = memberToken("lastcopy" + round + "a");
			String second = memberToken("lastcopy" + round + "b");

			List<Integer> statuses = Concurrently.run(List.of(
					() -> borrowStatus(first, bookId),
					() -> borrowStatus(second, bookId)));

			assertThat(statuses).containsExactlyInAnyOrder(201, 409);
			assertThat(availableQuantity(bookId)).isZero();
		}
	}

	private long createBook(String isbn, int quantity) {
		return bookCommandService.create(new BookRequest(isbn, "동시성 도서 " + isbn, "저자", "출판사", "IT", quantity)).id();
	}

	private String memberToken(String username) throws Exception {
		signup(username, PASSWORD);
		return bearer(username, PASSWORD);
	}

	private long borrow(String token, long bookId) throws Exception {
		String body = mockMvc.perform(post("/api/loans").header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"bookId\":" + bookId + "}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readValue(body, JsonNode.class).get("id").asLong();
	}

	private int borrowStatus(String token, long bookId) throws Exception {
		return mockMvc.perform(post("/api/loans").header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"bookId\":" + bookId + "}"))
				.andReturn().getResponse().getStatus();
	}

	private int returnStatus(String url, String token) throws Exception {
		return mockMvc.perform(post(url).header("Authorization", token)).andReturn().getResponse().getStatus();
	}

	private int availableQuantity(long bookId) {
		return bookRepository.findById(bookId).orElseThrow().getAvailableQuantity();
	}
}
