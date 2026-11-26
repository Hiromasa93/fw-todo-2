package com.example.todoapp.api;

import java.net.URI;
import java.util.List;
import java.util.Map;
import com.example.todoapp.Todo;
import com.example.todoapp.TodoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class QuickTodoApiController {
    private final TodoService todos;

    public QuickTodoApiController(TodoService todos) {
        this.todos = todos;
    }

    public record QuickTodoRequest(
            @NotBlank(message = "やることを入力してください")
            @Size(max = 255, message = "やることは255文字以内で入力してください") String title) {
        public QuickTodoRequest {
            title = title == null ? null : title.strip();
        }
    }

    @PostMapping("/api/todo")
    public ResponseEntity<?> create(@Valid @RequestBody QuickTodoRequest request, BindingResult errors) {
        if (errors.hasErrors()) {
            List<Map<String, String>> messages = errors.getFieldErrors().stream()
                    .map(error -> Map.of("field", error.getField(), "message", error.getDefaultMessage())).toList();
            return ResponseEntity.badRequest().body(Map.of("errors", messages));
        }
        Todo todo = new Todo();
        todo.setTitle(request.title());
        todo.setCategory("未分類");
        todo.setPriority(2);
        todo.setCompleted(false);
        todos.create(todo);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/todos/{id}").buildAndExpand(todo.getId()).toUri();
        return ResponseEntity.created(location).body(TodoDto.from(todos.findById(todo.getId())));
    }
}
