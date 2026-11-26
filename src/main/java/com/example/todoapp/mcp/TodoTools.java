package com.example.todoapp.mcp;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.example.todoapp.Todo;
import com.example.todoapp.TodoService;
import com.example.todoapp.api.HolidayClient;
import com.example.todoapp.api.TodoDto;

@Component
public class TodoTools {

    private final TodoService todoService;
    private final HolidayClient holidayClient;

    public TodoTools(TodoService todoService, HolidayClient holidayClient) {
        this.todoService = todoService;
        this.holidayClient = holidayClient;
    }

    @McpTool(name = "list_todos", description = "やることの一覧を返す（期間・ジャンルで絞れる）")
    public List<TodoDto> listTodos(
            @McpToolParam(description = "キーワード", required = false) String keyword,
            @McpToolParam(description = "ジャンル", required = false) String category,
            @McpToolParam(description = "開始日（yyyy-MM-dd）", required = false) String from,
            @McpToolParam(description = "終了日（yyyy-MM-dd）", required = false) String to) {
        LocalDate fromDate = from == null ? null : LocalDate.parse(from);
        LocalDate toDate = to == null ? null : LocalDate.parse(to);

        return todoService.search(keyword, category, "asc", fromDate, toDate).stream()
                .map(TodoDto::from)
                .toList();
    }

    @McpTool(name = "get_todo", description = "やることを1件返す")
    public TodoDto getTodo(
            @McpToolParam(description = "やることのID", required = true) Long id) {
        Todo todo = todoService.findById(id);
        return todo == null ? null : TodoDto.from(todo);
    }

    @McpTool(name = "add_todo", description = "やることを1件足す")
    public TodoDto addTodo(
            @McpToolParam(description = "やること", required = true) String title,
            @McpToolParam(description = "ジャンル（デザイン・マーケティング・プログラミング・資格・就職活動）", required = true) String category,
            @McpToolParam(description = "優先度", required = true) Integer priority,
            @McpToolParam(description = "メモ", required = false) String detail,
            @McpToolParam(description = "期限（yyyy-MM-dd）", required = false) String dueDate) {
        validateCategory(category);
        Todo todo = new Todo();
        todo.setTitle(title);
        todo.setCategory(category);
        todo.setPriority(priority);
        todo.setDetail(detail);
        todo.setDueDate(dueDate == null ? null : LocalDate.parse(dueDate));
        todoService.create(todo);
        return TodoDto.from(todo);
    }

    @McpTool(name = "update_todo", description = "やることを1件直す（期限を変えるのもこれ）")
    public TodoDto updateTodo(
            @McpToolParam(description = "やることのID", required = true) Long id,
            @McpToolParam(description = "やること", required = false) String title,
            @McpToolParam(description = "ジャンル（デザイン・マーケティング・プログラミング・資格・就職活動）", required = false) String category,
            @McpToolParam(description = "優先度", required = false) Integer priority,
            @McpToolParam(description = "メモ", required = false) String detail,
            @McpToolParam(description = "期限（yyyy-MM-dd。期限を消す場合は空文字）", required = false) String dueDate) {
        Todo todo = todoService.findById(id);
        if (todo == null) return null;
        if (title != null) todo.setTitle(title);
        if (category != null) { validateCategory(category); todo.setCategory(category); }
        if (priority != null) todo.setPriority(priority);
        if (detail != null) todo.setDetail(detail);
        if (dueDate != null) todo.setDueDate(dueDate.isBlank() ? null : LocalDate.parse(dueDate));
        todoService.update(todo);
        return TodoDto.from(todo);
    }

    @McpTool(name = "complete_todo", description = "やることを完了にする")
    public TodoDto completeTodo(
            @McpToolParam(description = "やることのID", required = true) Long id) {
        Todo todo = todoService.findById(id);
        if (todo == null) return null;
        todo.setCompleted(true);
        todoService.update(todo);
        return TodoDto.from(todo);
    }

    @McpTool(name = "delete_todo", description = "やることを1件消す")
    public int deleteTodo(
            @McpToolParam(description = "やることのID", required = true) Long id) {
        return todoService.delete(id);
    }

    @McpTool(name = "find_free_days", description = "期間の中で、期限のやることが無く、土日でも祝日でもない「空いている日」を返す。やることの期限を動かす先を決めるのに使う")
    public List<LocalDate> findFreeDays(
            @McpToolParam(description = "開始日（yyyy-MM-dd）", required = true) String from,
            @McpToolParam(description = "終了日（yyyy-MM-dd）", required = true) String to) {
        LocalDate start = LocalDate.parse(from);
        LocalDate end = LocalDate.parse(to);
        Set<LocalDate> busyDates = todoService.search(null, null, "asc", null, null).stream()
                .map(Todo::getDueDate).filter(date -> date != null).collect(Collectors.toSet());
        Set<String> holidays = holidayClient.getHolidays().holidays().keySet();
        List<LocalDate> freeDays = new java.util.ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            DayOfWeek day = date.getDayOfWeek();
            if (!busyDates.contains(date) && day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY
                    && !holidays.contains(date.toString())) {
                freeDays.add(date);
            }
        }
        return freeDays;
    }

    @McpTool(name = "summarize_week", description = "期限が指定期間内のやることを数え、総件数（完了済みを含む）・ジャンル別件数・期限切れ件数を要約して返す。一覧は返さない。期限切れは未完了で期限が日本時間の今日より前のもの。開始日・終了日はそれぞれ省略すると、その側の期限を制限しない")
    public TodoSummary summarizeWeek(
            @McpToolParam(description = "期限の開始日（yyyy-MM-dd、当日を含む。省略時は下限なし）", required = false) String from,
            @McpToolParam(description = "期限の終了日（yyyy-MM-dd、当日を含む。省略時は上限なし）", required = false) String to) {
        LocalDate fromDate = from == null ? null : LocalDate.parse(from);
        LocalDate toDate = to == null ? null : LocalDate.parse(to);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("開始日は終了日以前の日付にしてください");
        }

        List<Todo> todos = todoService.search(null, null, "asc", fromDate, toDate);
        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        for (String category : List.of("デザイン", "マーケティング", "プログラミング", "資格", "就職活動")) {
            categoryCounts.put(category, 0L);
        }
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));
        long overdueCount = 0;
        for (Todo todo : todos) {
            categoryCounts.merge(todo.getCategory(), 1L, Long::sum);
            if (!Boolean.TRUE.equals(todo.getCompleted()) && todo.getDueDate() != null
                    && todo.getDueDate().isBefore(today)) {
                overdueCount++;
            }
        }
        return new TodoSummary(todos.size(), categoryCounts, overdueCount);
    }

    public record TodoSummary(long totalCount, Map<String, Long> categoryCounts, long overdueCount) {
    }

    private void validateCategory(String category) {
        if (!List.of("デザイン", "マーケティング", "プログラミング", "資格", "就職活動").contains(category)) {
            throw new IllegalArgumentException("ジャンルは指定された5種類から選んでください");
        }
    }
}
