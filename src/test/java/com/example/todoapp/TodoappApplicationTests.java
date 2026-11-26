package com.example.todoapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.net.SocketTimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import org.springframework.transaction.annotation.Transactional;

import com.example.todoapp.api.TodoDto;
import com.example.todoapp.api.WeatherClient;
import com.example.todoapp.api.WeatherApiController;

@SpringBootTest
@AutoConfigureMockMvc
class TodoappApplicationTests {

	@Autowired
	TodoService todoService;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	MockMvc mockMvc;

	@Test
	void weatherApiFiltersDatesAndConvertsWeatherCodes() throws Exception {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(
				"https://api.open-meteo.com/v1/forecast?latitude=35.6812&longitude=139.7671"
				+ "&daily=weather_code&timezone=Asia%2FTokyo&forecast_days=16"))
				.andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
						"{\"daily\":{\"time\":[\"2026-11-26\",\"2026-11-27\"],\"weather_code\":[0,61]}}", MediaType.APPLICATION_JSON));
		MockMvc weatherMvc = MockMvcBuilders.standaloneSetup(new WeatherApiController(new WeatherClient(builder.build()))).build();
		var response = weatherMvc.perform(get("/api/weather").param("from", "2026-11-27").param("to", "2026-11-27"))
				.andExpect(status().isOk()).andReturn().getResponse();
		assertThat(response.getContentAsString(StandardCharsets.UTF_8)).isEqualTo("{\"2026-11-27\":\"雨\"}");
		assertThat(response.getHeader("X-Weather-Unavailable")).isNull();
		server.verify();
	}

	@Test
	void weatherTimeoutReturnsEmptyMapAndCalendarStillRenders() throws Exception {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything())
				.andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withException(new SocketTimeoutException()));
		MockMvc weatherMvc = MockMvcBuilders.standaloneSetup(new WeatherApiController(new WeatherClient(builder.build()))).build();
		var response = weatherMvc.perform(get("/api/weather")).andExpect(status().isOk()).andReturn().getResponse();
		assertThat(response.getContentAsString()).isEqualTo("{}");
		assertThat(response.getHeader("X-Weather-Unavailable")).isEqualTo("true");
		String calendar = mockMvc.perform(get("/calendar")).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertThat(calendar).contains("calendar-table", "showWeatherInCalendar();", "showTodosInCalendar();");
		server.verify();
	}

	@Test
	void weatherInvalidResponseAndServerErrorAreHandled() {
		for (String body : List.of("invalid-json", "{}", "{\"daily\":{\"time\":[\"bad-date\"],\"weather_code\":[0]}}",
				"{\"daily\":{\"time\":[\"2026-11-26\"],\"weather_code\":[]}}")) {
			RestClient.Builder builder = RestClient.builder();
			MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
			server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything())
					.andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(body, MediaType.APPLICATION_JSON));
			var result = new WeatherClient(builder.build()).getWeather();
			assertThat(result.unavailable()).isTrue();
			assertThat(result.weather()).isEmpty();
			server.verify();
		}
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything())
				.andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());
		assertThat(new WeatherClient(builder.build()).getWeather().unavailable()).isTrue();
		server.verify();
	}

	@Test
	@Transactional
	void csvDownloadPreservesFiltersSortEncodingAndLiteralText() throws Exception {
		String keyword = "csv-check-" + UUID.randomUUID();
		for (int i = 0; i < 12; i++) {
			Todo todo = new Todo();
			todo.setTitle((i % 2 == 0 ? "=" : "+") + keyword + ",\"日本語\"\r\n" + i);
			todo.setDetail("=SUM(1,2)");
			todo.setCategory("資格");
			todo.setPriority(1);
			todo.setDueDate(LocalDate.of(2026, 12, i + 1));
			todo.setCompleted(i == 0);
			todoService.create(todo);
		}
		var response = mockMvc.perform(get("/api/todos.csv").param("keyword", keyword)
				.param("category", "資格").param("order", "desc"))
				.andExpect(status().isOk()).andReturn().getResponse();
		assertThat(response.getHeader("Content-Disposition")).isEqualTo("attachment; filename=\"todos.csv\"");
		assertThat(response.getContentType()).isEqualTo("text/csv;charset=UTF-8");
		assertThat(response.getContentAsByteArray()).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
		String csv = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
		assertThat(csv).startsWith("\uFEFFやること,ジャンル,優先度,期限,状態\r\n");
		assertThat(csv).contains("\"'+" + keyword + ",\"\"日本語\"\"\r\n11\"");
		assertThat(csv).contains("\"'=" + keyword + ",\"\"日本語\"\"\r\n10\"");
		assertThat(csv).contains("\"高\",\"2026/12/12\",\"未完了\"");
		assertThat(csv).doesNotContain("2026/12/01");
		assertThat(csv.indexOf("2026/12/12")).isLessThan(csv.indexOf("2026/12/02"));
		assertThat(csv.split("\"未完了\"", -1)).hasSize(12);
		String all = mockMvc.perform(get("/api/todos.csv").param("keyword", keyword)
				.param("includeCompleted", "true").param("order", "asc"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertThat(all).contains("2026/12/01", "\"完了\"");
		assertThat(all.indexOf("2026/12/01")).isLessThan(all.indexOf("2026/12/12"));
		String empty = mockMvc.perform(get("/api/todos.csv").param("keyword", keyword).param("category", "デザイン"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertThat(empty).isEqualTo("\uFEFFやること,ジャンル,優先度,期限,状態\r\n");
	}

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void completionTimeChangesOnlyWhenCompletionStateChanges() {
		Todo todo = new Todo();
		todo.setTitle("completion-check-" + UUID.randomUUID());
		todo.setCategory("資格");
		todo.setPriority(2);
		todo.setCompleted(false);
		todoService.create(todo);
		assertThat(todoService.findById(todo.getId()).getCompletedAt()).isNull();
		todo.setTitle(todo.getTitle() + "-edited");
		todoService.update(todo);
		assertThat(todoService.findById(todo.getId()).getCompletedAt()).isNull();
		LocalDateTime before = jdbcTemplate.queryForObject("SELECT CURRENT_TIMESTAMP", LocalDateTime.class);
		todo.setCompleted(true);
		todoService.update(todo);
		LocalDateTime after = jdbcTemplate.queryForObject("SELECT CURRENT_TIMESTAMP", LocalDateTime.class);
		assertThat(todo.getCompletedAt()).isBetween(before, after);
		assertThat(TodoDto.from(todoService.findById(todo.getId())).getCompletedAt()).isEqualTo(todo.getCompletedAt());
		LocalDateTime originalTime = LocalDateTime.of(2026, 4, 5, 12, 34, 56);
		jdbcTemplate.update("UPDATE todos SET completed_at = ? WHERE id = ?", originalTime, todo.getId());
		todo.setTitle(todo.getTitle() + "-again");
		todoService.update(todo);
		assertThat(todo.getCompletedAt()).isEqualTo(originalTime);
		todoService.update(todo);
		assertThat(todoService.findById(todo.getId()).getCompletedAt()).isEqualTo(originalTime);
		todo.setCompleted(false);
		todoService.update(todo);
		assertThat(todo.getCompletedAt()).isNull();
		assertThat(todoService.findById(todo.getId()).getCompletedAt()).isNull();
		todo.setCompleted(true);
		todoService.update(todo);
		assertThat(todo.getCompletedAt()).isNotNull().isNotEqualTo(originalTime);
	}

	@Test
	@Transactional
	void creatingCompletedTodoDoesNotInventCompletionTime() {
		Todo todo = new Todo();
		todo.setTitle("completed-create-check-" + UUID.randomUUID());
		todo.setCategory("資格");
		todo.setPriority(2);
		todo.setCompleted(true);
		todoService.create(todo);
		assertThat(todoService.findById(todo.getId()).getCompletedAt()).isNull();
		todo.setTitle(todo.getTitle() + "-edited");
		todoService.update(todo);
		assertThat(todoService.findById(todo.getId()).getCompletedAt()).isNull();
	}

	@Test
	@Transactional
	void filteredPagesHaveTenRowsAndKeepStableOrderForEqualDueDates() {
		String keyword = "page-check-" + UUID.randomUUID();
		List<Long> ids = new ArrayList<>();
		for (int i = 0; i < 11; i++) {
			Todo todo = new Todo();
			todo.setTitle(keyword + "-" + i);
			todo.setCategory("資格");
			todo.setPriority(2);
			todo.setDueDate(LocalDate.of(2026, 12, 1));
			todo.setCompleted(false);
			todoService.create(todo);
			ids.add(todo.getId());
		}
		for (String order : List.of("asc", "desc")) {
			var first = todoService.searchPage(keyword, "資格", order, false, false, 1);
			var second = todoService.searchPage(keyword, "資格", order, false, false, 2);
			assertThat(first.totalPages()).isEqualTo(2);
			assertThat(first.todos()).extracting(Todo::getId).containsExactlyElementsOf(ids.subList(0, 10));
			assertThat(second.todos()).extracting(Todo::getId).containsExactly(ids.get(10));
		}
		assertThat(todoService.searchPage(keyword, "資格", "asc", false, false, 0).currentPage()).isEqualTo(1);
		assertThat(todoService.searchPage(keyword, "資格", "asc", false, false, 999).currentPage()).isEqualTo(2);
		assertThat(todoService.searchPage(keyword, "デザイン", "asc", false, false, 1).todos()).isEmpty();
		Todo completed = todoService.findById(ids.get(0));
		completed.setCompleted(true);
		todoService.update(completed);
		assertThat(todoService.searchPage(keyword, "資格", "asc", false, false, 1).totalPages()).isEqualTo(1);
		assertThat(todoService.searchPage(keyword, "資格", "asc", true, false, 1).totalPages()).isEqualTo(2);
		todoService.delete(ids.get(0));
		assertThat(todoService.searchPage(keyword, "資格", "asc", true, false, 1).todos()).hasSize(10);
		assertThat(todoService.searchPage(keyword, "資格", "asc", false, true, 1).todos())
				.extracting(Todo::getId).containsExactly(ids.get(0));
	}

}
