package com.example.todoapp;

import java.text.Normalizer;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class SmartDateParser {
    private static final Pattern RELATIVE = Pattern.compile("([0-9]{1,4})日(後|前)");
    private static final Pattern WEEKDAY = Pattern.compile("(今週|来週|再来週)([月火水木金土日])(?:曜(?:日)?)?");
    private static final Pattern JAPANESE_DATE = Pattern.compile("(?:([0-9]{4})年)?([0-9]{1,2})月([0-9]{1,2})日");
    private static final Pattern NUMERIC_DATE = Pattern.compile("([0-9]{4})([-/])([0-9]{1,2})\\2([0-9]{1,2})");
    private final Clock clock;

    public SmartDateParser() {
        this(Clock.system(ZoneId.of("Asia/Tokyo")));
    }

    public SmartDateParser(Clock clock) {
        this.clock = clock;
    }

    public LocalDate parse(String input) {
        if (input == null || input.isBlank() || input.length() > 100) {
            throw new IllegalArgumentException("日付を100文字以内で入力してください。");
        }
        String text = Normalizer.normalize(input, Normalizer.Form.NFKC).replaceAll("[\\s\\p{Z}]+", "");
        LocalDate today = LocalDate.now(clock);
        try {
            LocalDate result = switch (text) {
                case "今日" -> today;
                case "明日" -> today.plusDays(1);
                case "明後日", "あさって" -> today.plusDays(2);
                case "昨日" -> today.minusDays(1);
                default -> match(text, today);
            };
            if (result.getYear() < 1 || result.getYear() > 9999) throw new DateTimeException("Year out of range");
            return result;
        } catch (DateTimeException | NumberFormatException exception) {
            throw new IllegalArgumentException("存在する日付を入力してください。", exception);
        }
    }

    private LocalDate match(String text, LocalDate today) {
        Matcher relative = RELATIVE.matcher(text);
        if (relative.matches()) {
            int days = Integer.parseInt(relative.group(1));
            return today.plusDays(relative.group(2).equals("後") ? days : -days);
        }
        Matcher weekday = WEEKDAY.matcher(text);
        if (weekday.matches()) {
            int weeks = switch (weekday.group(1)) { case "来週" -> 1; case "再来週" -> 2; default -> 0; };
            int day = "月火水木金土日".indexOf(weekday.group(2));
            return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(weeks).plusDays(day);
        }
        Matcher japanese = JAPANESE_DATE.matcher(text);
        if (japanese.matches()) {
            int month = Integer.parseInt(japanese.group(2));
            int day = Integer.parseInt(japanese.group(3));
            if (japanese.group(1) != null) return LocalDate.of(Integer.parseInt(japanese.group(1)), month, day);
            // Validate against a leap year before searching for the next occurrence.
            LocalDate.of(2000, month, day);
            for (int year = today.getYear(); year <= Math.min(9999, today.getYear() + 8); year++) {
                try {
                    LocalDate candidate = LocalDate.of(year, month, day);
                    if (!candidate.isBefore(today)) return candidate;
                } catch (DateTimeException ignored) {
                    // February 29 may require advancing to the next leap year.
                }
            }
            throw new DateTimeException("No occurrence in supported range");
        }
        Matcher numeric = NUMERIC_DATE.matcher(text);
        if (numeric.matches()) return LocalDate.of(Integer.parseInt(numeric.group(1)),
                Integer.parseInt(numeric.group(3)), Integer.parseInt(numeric.group(4)));
        throw new IllegalArgumentException("日付を解釈できません。例：明日、3日後、来週金曜、12月25日");
    }
}
