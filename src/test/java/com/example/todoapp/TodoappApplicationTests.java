package com.example.todoapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.example.todoapp.api.TodoDto;

@SpringBootTest
class TodoappApplicationTests {

	@Autowired
	TodoService todoService;

	@Autowired
	JdbcTemplate jdbcTemplate;

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
