package com.example.library.loan.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.library.book.domain.BookRepository;
import com.example.library.support.IntegrationTestSupport;
import com.example.library.user.domain.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

class LoanIntegrationTest extends IntegrationTestSupport {

	private static final String PASSWORD = "Passw0rd!";

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Clock clock;

	@Test
	@DisplayName("대출하면 재고가 줄고 내 대출에 보이며, 반납하면 재고가 돌아온다")
	void borrowAndReturn() throws Exception {
		signup("loanuser01", PASSWORD);
		String token = bearer("loanuser01", PASSWORD);
		int before = bookRepository.findById(2L).orElseThrow().getAvailableQuantity();

		long loanId = idOf(borrow(token, 2L).andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("LOANED")));
		assertThat(bookRepository.findById(2L).orElseThrow().getAvailableQuantity()).isEqualTo(before - 1);

		mockMvc.perform(get("/api/loans/me").header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(loanId))
				.andExpect(jsonPath("$[0].dueDate").value(LocalDate.now(clock).plusDays(14).toString()));

		mockMvc.perform(post("/api/loans/" + loanId + "/return").header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RETURNED"));
		assertThat(bookRepository.findById(2L).orElseThrow().getAvailableQuantity()).isEqualTo(before);
	}

	@Test
	@DisplayName("미반납 5권이면 6번째 대출은 409 LOAN_LIMIT_EXCEEDED")
	void loanLimit() throws Exception {
		signup("loanuser02", PASSWORD);
		String token = bearer("loanuser02", PASSWORD);
		for (long bookId : new long[] {3, 4, 5, 7, 9}) {
			borrow(token, bookId).andExpect(status().isCreated());
		}

		borrow(token, 10L)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("LOAN_LIMIT_EXCEEDED"));
	}

	@Test
	@DisplayName("연체 중인 대출이 있으면 409 OVERDUE_LOAN_EXISTS")
	void overdueBlocksBorrow() throws Exception {
		signup("loanuser03", PASSWORD);
		Long userId = userRepository.findByUsername("loanuser03").orElseThrow().getId();
		jdbcTemplate.update("INSERT INTO loans (user_id, book_id, loan_date, due_date, status) VALUES (?, ?, ?, ?, ?)",
				userId, 11L, LocalDate.now(clock).minusDays(20), LocalDate.now(clock).minusDays(6), "LOANED");
		String token = bearer("loanuser03", PASSWORD);

		mockMvc.perform(get("/api/loans/me").header("Authorization", token))
				.andExpect(jsonPath("$[0].status").value("OVERDUE"));
		borrow(token, 12L)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("OVERDUE_LOAN_EXISTS"));
	}

	@Test
	@DisplayName("남의 대출은 반납할 수 없다(404), 사서는 내 대출 API 권한이 없다(403)")
	void cannotReturnOthersLoan() throws Exception {
		signup("loanuser04", PASSWORD);
		signup("loanuser05", PASSWORD);
		long loanId = idOf(borrow(bearer("loanuser04", PASSWORD), 13L).andExpect(status().isCreated()));

		mockMvc.perform(post("/api/loans/" + loanId + "/return").header("Authorization", bearer("loanuser05", PASSWORD)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("LOAN_NOT_FOUND"));
		mockMvc.perform(get("/api/loans/me").header("Authorization", bearer("librarian", LIBRARIAN_PASSWORD)))
				.andExpect(status().isForbidden());
	}

	private ResultActions borrow(String token, long bookId) throws Exception {
		return mockMvc.perform(post("/api/loans").header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON).content("{\"bookId\":" + bookId + "}"));
	}

	private long idOf(ResultActions actions) throws Exception {
		return objectMapper.readValue(actions.andReturn().getResponse().getContentAsString(), JsonNode.class)
				.get("id").asLong();
	}
}
