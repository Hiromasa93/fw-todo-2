package com.example.todoapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class TodoappApplication {

	@Bean
	ApplicationRunner ensureDeletedAtColumn(JdbcTemplate jdbcTemplate) {
		return args -> {
			Integer columnCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
							+ "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'todos' "
							+ "AND COLUMN_NAME = 'deleted_at'",
					Integer.class);
			if (columnCount != null && columnCount == 0) {
				jdbcTemplate.execute("ALTER TABLE todos ADD COLUMN deleted_at DATETIME NULL");
			}
			Integer pinnedColumnCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
							+ "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'todos' "
							+ "AND COLUMN_NAME = 'pinned'",
					Integer.class);
			if (pinnedColumnCount != null && pinnedColumnCount == 0) {
				jdbcTemplate.execute("ALTER TABLE todos ADD COLUMN pinned BOOLEAN NOT NULL DEFAULT FALSE");
			}
			Integer completedAtColumnCount = jdbcTemplate.queryForObject(
					"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
							+ "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'todos' "
							+ "AND COLUMN_NAME = 'completed_at'",
					Integer.class);
			if (completedAtColumnCount != null && completedAtColumnCount == 0) {
				jdbcTemplate.execute("ALTER TABLE todos ADD COLUMN completed_at DATETIME NULL");
				jdbcTemplate.update("UPDATE todos SET completed_at = ? "
						+ "WHERE id = 1 AND completed = true AND completed_at IS NULL", "2026-04-05 00:00:00");
			}
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(TodoappApplication.class, args);
	}

}
