package com.example.todoapp.api;

import java.util.List;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import jakarta.validation.Valid;

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
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return todoService.search(keyword, category, order, from, to).stream()
                .map(TodoDto::from)
                .toList();
    }

    @GetMapping(value = "/api/todos.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> csv(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(defaultValue = "false") boolean includeCompleted,
            @RequestParam(required = false) String trash) {
        String sortOrder = "desc".equals(order) ? "desc" : "asc";
        List<Todo> todos = "1".equals(trash)
                ? todoService.searchTrash(keyword, category, sortOrder, null, null)
                : todoService.search(keyword, category, sortOrder, null, null, includeCompleted);
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        StringBuilder csv = new StringBuilder("\uFEFFやること,ジャンル,優先度,期限,状態\r\n");
        for (Todo todo : todos) {
            String priority = Integer.valueOf(1).equals(todo.getPriority()) ? "高"
                    : Integer.valueOf(2).equals(todo.getPriority()) ? "中" : "低";
            String dueDate = todo.getDueDate() == null ? "期限なし" : todo.getDueDate().format(dateFormat);
            String status = Boolean.TRUE.equals(todo.getCompleted()) ? "完了" : "未完了";
            if (Boolean.TRUE.equals(todo.getCompleted()) && todo.getCompletedAt() != null) {
                status += " " + todo.getCompletedAt().format(dateFormat);
            }
            csv.append(csvCell(todo.getTitle())).append(',')
                    .append(csvCell(todo.getCategory())).append(',')
                    .append(csvCell(priority)).append(',')
                    .append(csvCell(dueDate)).append(',')
                    .append(csvCell(status)).append("\r\n");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"todos.csv\"")
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String csvCell(String value) {
        String text = value == null ? "" : value;
        int first = text.codePoints()
                .dropWhile(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)
                        || Character.isISOControl(c) || c == 0xFEFF)
                .findFirst().orElse(-1);
        if (first >= 0 && "=+-@＝＋－＠".indexOf(first) >= 0) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
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
    public ResponseEntity<?> create(@Valid @RequestBody TodoRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return badRequest(bindingResult);
        }
        Todo todo = toTodo(request);
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
    public ResponseEntity<?> replace(@PathVariable Long id, @Valid @RequestBody TodoRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return badRequest(bindingResult);
        }
        if (todoService.findById(id) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFound(id));
        }
        Todo todo = toTodo(request);
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

    private Todo toTodo(TodoRequest request) {
        Todo todo = new Todo();
        todo.setTitle(request.getTitle());
        todo.setDetail(request.getDetail());
        todo.setCategory(request.getCategory());
        todo.setPriority(request.getPriority());
        todo.setDueDate(request.getDueDate());
        todo.setCompleted(request.getCompleted());
        return todo;
    }

    private ResponseEntity<ProblemDetail> badRequest(BindingResult bindingResult) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "入力に誤りがあります");
        problem.setTitle("Bad Request");
        problem.setInstance(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri());
        List<Map<String, String>> errors = bindingResult.getFieldErrors().stream()
                .map(this::toError)
                .toList();
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    private Map<String, String> toError(FieldError error) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("field", error.getField());
        item.put("message", error.getDefaultMessage());
        return item;
    }
}
