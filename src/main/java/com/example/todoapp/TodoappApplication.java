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
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(TodoappApplication.class, args);
	}

}
