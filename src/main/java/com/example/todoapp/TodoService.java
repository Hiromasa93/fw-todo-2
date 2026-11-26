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
        return todoMapper.search(keyword, category, order, from, to, true);
    }

    public List<Todo> search(String keyword, String category, String order, LocalDate from, LocalDate to,
            boolean includeCompleted) {
        return todoMapper.search(keyword, category, order, from, to, includeCompleted);
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

    public int delete(Long id) {
        log.info("Deleting todo id={}", id);
        return todoMapper.deleteById(id);
    }
}
