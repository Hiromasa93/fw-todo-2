package com.example.todoapp;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CalendarController {

    @GetMapping("/calendar")
    public String calendar(@RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "month") String view,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth displayedMonth = date != null ? YearMonth.from(date) : YearMonth.of(
                year != null ? year : currentMonth.getYear(),
                month != null ? month : currentMonth.getMonthValue());
        LocalDate focusDate = date != null ? date
                : displayedMonth.equals(currentMonth) ? today : displayedMonth.atDay(1);
        boolean weekView = "week".equals(view);

        LocalDate firstDay = displayedMonth.atDay(1);
        LocalDate lastDay = displayedMonth.atEndOfMonth();
        int leadingBlankDays = firstDay.getDayOfWeek().getValue() % 7;
        int cellCount = ((leadingBlankDays + displayedMonth.lengthOfMonth() + 6) / 7) * 7;
        List<List<LocalDate>> weeks = new ArrayList<>();

        if (weekView) {
            firstDay = focusDate.minusDays(focusDate.getDayOfWeek().getValue() % 7);
            lastDay = firstDay.plusDays(6);
            List<LocalDate> week = new ArrayList<>();
            for (int day = 0; day < 7; day++) {
                week.add(firstDay.plusDays(day));
            }
            weeks.add(week);
        } else {
            for (int cell = 0; cell < cellCount; cell++) {
                if (cell % 7 == 0) {
                    weeks.add(new ArrayList<>());
                }
                LocalDate cellDate = cell >= leadingBlankDays && cell < leadingBlankDays + displayedMonth.lengthOfMonth()
                        ? firstDay.plusDays(cell - leadingBlankDays)
                        : null;
                weeks.get(weeks.size() - 1).add(cellDate);
            }
        }

        YearMonth previousMonth = displayedMonth.minusMonths(1);
        YearMonth nextMonth = displayedMonth.plusMonths(1);
        model.addAttribute("year", displayedMonth.getYear());
        model.addAttribute("month", displayedMonth.getMonthValue());
        model.addAttribute("monthName", displayedMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.JAPANESE));
        model.addAttribute("from", firstDay);
        model.addAttribute("to", lastDay);
        model.addAttribute("weeks", weeks);
        model.addAttribute("weekView", weekView);
        model.addAttribute("focusDate", focusDate);
        model.addAttribute("previousWeek", firstDay.minusWeeks(1));
        model.addAttribute("nextWeek", firstDay.plusWeeks(1));
        model.addAttribute("previousYear", previousMonth.getYear());
        model.addAttribute("previousMonth", previousMonth.getMonthValue());
        model.addAttribute("nextYear", nextMonth.getYear());
        model.addAttribute("nextMonth", nextMonth.getMonthValue());
        return "calendar";
    }
}

