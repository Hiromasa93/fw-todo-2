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
