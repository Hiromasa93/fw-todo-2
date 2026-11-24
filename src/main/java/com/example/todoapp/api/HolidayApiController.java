package com.example.todoapp.api;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HolidayApiController {

    private final HolidayClient holidayClient;

    public HolidayApiController(HolidayClient holidayClient) {
        this.holidayClient = holidayClient;
    }

    @GetMapping("/api/holidays")
    public ResponseEntity<Map<String, String>> getHolidays(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        HolidayClient.HolidayResult result = holidayClient.getHolidays();
        Map<String, String> holidays = result.holidays();

        if (from == null && to == null) {
            return holidayResponse(holidays, result.unavailable());
        }

        Map<String, String> filteredHolidays = holidays.entrySet().stream()
                .filter(entry -> {
                    LocalDate date = LocalDate.parse(entry.getKey());
                    return (from == null || !date.isBefore(from))
                            && (to == null || !date.isAfter(to));
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, second) -> first,
                        java.util.LinkedHashMap::new));
        return holidayResponse(filteredHolidays, result.unavailable());
    }

    private ResponseEntity<Map<String, String>> holidayResponse(Map<String, String> holidays, boolean unavailable) {
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (unavailable) {
            response.header("X-Holidays-Unavailable", "true");
        }
        return response.body(holidays);
    }
}
