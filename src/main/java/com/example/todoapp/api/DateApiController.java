package com.example.todoapp.api;

import java.time.LocalDate;
import com.example.todoapp.SmartDateParser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DateApiController {
    private final SmartDateParser parser;

    public DateApiController(SmartDateParser parser) {
        this.parser = parser;
    }

    public record ParseResult(boolean success, LocalDate date, String message) {}

    @GetMapping("/api/date/parse")
    public ResponseEntity<ParseResult> parse(@RequestParam(defaultValue = "") String input) {
        try {
            return ResponseEntity.ok(new ParseResult(true, parser.parse(input), ""));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(new ParseResult(false, null, exception.getMessage()));
        }
    }
}
