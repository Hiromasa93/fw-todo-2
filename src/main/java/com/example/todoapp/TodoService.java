package com.example.todoapp;

import java.util.List;
import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TodoService {

    private final TodoMapper todoMapper;

    public TodoService(TodoMapper todoMapper) {
        this.todoMapper = todoMapper;
    }

    public List<Todo> search(String keyword, String category, String order, LocalDate from, LocalDate to) {
        return todoMapper.search(keyword, category, order, from, to, true, false);
    }

    public List<Todo> search(String keyword, String category, String order, LocalDate from, LocalDate to,
            boolean includeCompleted) {
        return todoMapper.search(keyword, category, order, from, to, includeCompleted, false);
    }

    public List<Todo> searchTrash(String keyword, String category, String order, LocalDate from, LocalDate to) {
        return todoMapper.search(keyword, category, order, from, to, true, true);
    }

    public TodoPage searchPage(String keyword, String category, String order,
            boolean includeCompleted, boolean trash, int page) {
        int pageSize = 10;
        boolean showCompleted = trash || includeCompleted;
        long totalCount = todoMapper.count(keyword, category, null, null, showCompleted, trash);
        int totalPages = (int) Math.max(1, (totalCount + pageSize - 1) / pageSize);
        int currentPage = Math.max(1, Math.min(page, totalPages));
        List<Todo> todos = todoMapper.searchPage(keyword, category, order, null, null,
                showCompleted, trash, pageSize, (long) (currentPage - 1) * pageSize);
        return new TodoPage(todos, currentPage, totalPages);
    }

    public record TodoPage(List<Todo> todos, int currentPage, int totalPages) {
    }

    public Todo findById(Long id) {
        return todoMapper.findById(id);
    }

    public int create(Todo todo) {
        log.info("Creating todo id={}", todo.getId());
        return todoMapper.insert(todo);
    }

    public int update(Todo todo) {
        log.info("Updating todo id={}", todo.getId());
        return todoMapper.update(todo);
    }

    public int togglePinned(Long id) {
        return todoMapper.togglePinned(id);
    }

    public int delete(Long id) {
        log.info("Deleting todo id={}", id);
        return todoMapper.deleteById(id);
    }

    public int restore(Long id) {
        log.info("Restoring todo id={}", id);
        return todoMapper.restoreById(id);
    }
}
