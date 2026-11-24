package com.example.todoapp.api;

import java.util.List;
import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.todoapp.Todo;
import com.example.todoapp.TodoService;

@RestController
public class TodoApiController {

    private final TodoService todoService;

    public TodoApiController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/api/todos")
    public List<TodoDto> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "asc") String order) {
        return todoService.search(keyword, category, order).stream()
                .map(TodoDto::from)
                .toList();
    }

    @GetMapping("/api/todos/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Todo todo = todoService.findById(id);
        if (todo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFound(id));
        }
        return ResponseEntity.ok(TodoDto.from(todo));
    }

    @PostMapping("/api/todos")
    public ResponseEntity<TodoDto> create(@RequestBody Todo todo) {
        if (todo.getCompleted() == null) {
            todo.setCompleted(false);
        }
        todoService.create(todo);
        Todo created = todoService.findById(todo.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(todo.getId()).toUri();
        return ResponseEntity.created(location).body(TodoDto.from(created));
    }

    @PutMapping("/api/todos/{id}")
    public ResponseEntity<?> replace(@PathVariable Long id, @RequestBody Todo todo) {
        if (todoService.findById(id) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFound(id));
        }
        todo.setId(id);
        todoService.update(todo);
        return ResponseEntity.ok(TodoDto.from(todoService.findById(id)));
    }

    @DeleteMapping("/api/todos/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (todoService.findById(id) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFound(id));
        }
        todoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ProblemDetail notFound(Long id) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "指定された Todo が見つかりません。");
        problem.setTitle("Not Found");
        problem.setInstance(URI.create("/api/todos/" + id));
        return problem;
    }
}
