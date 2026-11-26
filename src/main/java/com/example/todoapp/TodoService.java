package com.example.todoapp;

import java.util.List;
import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class TodoService {

    private final TodoMapper todoMapper;
    private final TagMapper tagMapper;
    private final TagService tagService;

    public TodoService(TodoMapper todoMapper, TagMapper tagMapper, TagService tagService) {
        this.todoMapper = todoMapper;
        this.tagMapper = tagMapper;
        this.tagService = tagService;
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
        Todo todo = todoMapper.findById(id);
        if (todo != null) {
            todo.setTags(tagMapper.findByTodoId(id));
            todo.setTagIds(todo.getTags().stream().map(Tag::getId).toList());
        }
        return todo;
    }

    @Transactional
    public int create(Todo todo) {
        log.info("Creating todo id={}", todo.getId());
        List<Tag> tags = tagService.resolve(todo.getTagIds());
        int inserted = todoMapper.insert(todo);
        saveTags(todo, tags);
        return inserted;
    }

    @Transactional
    public int update(Todo todo) {
        log.info("Updating todo id={}", todo.getId());
        List<Tag> tags = tagService.resolve(todo.getTagIds());
        int updated = todoMapper.update(todo);
        if (updated > 0) {
            if (todo.getTagIds() != null) {
                saveTags(todo, tags);
            }
            Todo saved = todoMapper.findById(todo.getId());
            if (saved != null) {
                todo.setCompletedAt(saved.getCompletedAt());
            }
        }
        return updated;
    }

    private void saveTags(Todo todo, List<Tag> tags) {
        tagMapper.deleteTodoTags(todo.getId());
        List<Long> ids = tags.stream().map(Tag::getId).toList();
        if (!ids.isEmpty()) {
            tagMapper.insertTodoTags(todo.getId(), ids);
        }
        todo.setTags(tags);
        todo.setTagIds(ids);
    }

    public int togglePinned(Long id) {
        return todoMapper.togglePinned(id);
    }

    public int delete(Long id) {
        log.info("Deleting todo id={}", id);
        return todoMapper.deleteById(id);
    }

    public int deleteAll(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return todoMapper.deleteByIds(ids);
    }

    public int updateStatusAll(List<Long> ids, boolean completed) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return todoMapper.updateStatusByIds(ids, completed);
    }

    public int restore(Long id) {
        log.info("Restoring todo id={}", id);
        return todoMapper.restoreById(id);
    }
}
