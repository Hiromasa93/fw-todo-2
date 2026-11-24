package com.example.todoapp;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CalendarController {

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month, Model model) {
        YearMonth currentMonth = YearMonth.now();
        YearMonth displayedMonth = YearMonth.of(
                year != null ? year : currentMonth.getYear(),
                month != null ? month : currentMonth.getMonthValue());

        LocalDate firstDay = displayedMonth.atDay(1);
        LocalDate lastDay = displayedMonth.atEndOfMonth();
        int leadingBlankDays = firstDay.getDayOfWeek().getValue() % 7;
        int cellCount = ((leadingBlankDays + displayedMonth.lengthOfMonth() + 6) / 7) * 7;
        List<List<LocalDate>> weeks = new ArrayList<>();

        for (int cell = 0; cell < cellCount; cell++) {
            if (cell % 7 == 0) {
                weeks.add(new ArrayList<>());
            }
            LocalDate date = cell >= leadingBlankDays && cell < leadingBlankDays + displayedMonth.lengthOfMonth()
                    ? firstDay.plusDays(cell - leadingBlankDays)
                    : null;
            weeks.get(weeks.size() - 1).add(date);
        }

        YearMonth previousMonth = displayedMonth.minusMonths(1);
        YearMonth nextMonth = displayedMonth.plusMonths(1);
        model.addAttribute("year", displayedMonth.getYear());
        model.addAttribute("month", displayedMonth.getMonthValue());
        model.addAttribute("monthName", displayedMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.JAPANESE));
        model.addAttribute("from", firstDay);
        model.addAttribute("to", lastDay);
        model.addAttribute("weeks", weeks);
        model.addAttribute("previousYear", previousMonth.getYear());
        model.addAttribute("previousMonth", previousMonth.getMonthValue());
        model.addAttribute("nextYear", nextMonth.getYear());
        model.addAttribute("nextMonth", nextMonth.getMonthValue());
        return "calendar";
    }
}

