package com.example.todoapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class TodoappApplicationTests {

	@Autowired
	TodoService todoService;

	@Test
	void contextLoads() {
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
