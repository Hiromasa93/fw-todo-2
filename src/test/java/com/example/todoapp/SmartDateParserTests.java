package com.example.todoapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class SmartDateParserTests {
    private SmartDateParser on(String instant) {
        return new SmartDateParser(Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Tokyo")));
    }

    @Test
    void parsesExamplesAndNormalizesFullWidthCharacters() {
        var parser = on("2026-11-26T00:00:00Z");
        assertThat(parser.parse("明日")).isEqualTo(LocalDate.of(2026, 11, 27));
        assertThat(parser.parse("３日後")).isEqualTo(LocalDate.of(2026, 11, 29));
        assertThat(parser.parse(" 来週 金曜日 ")).isEqualTo(LocalDate.of(2026, 12, 4));
        assertThat(parser.parse("12月25日")).isEqualTo(LocalDate.of(2026, 12, 25));
        assertThat(parser.parse("２０２７年１月２日")).isEqualTo(LocalDate.of(2027, 1, 2));
        assertThat(parser.parse("2027/1/2")).isEqualTo(LocalDate.of(2027, 1, 2));
        assertThat(parser.parse("2027-01-02")).isEqualTo(LocalDate.of(2027, 1, 2));
        assertThat(parser.parse("昨日")).isEqualTo(LocalDate.of(2026, 11, 25));
        assertThat(parser.parse("明後日")).isEqualTo(LocalDate.of(2026, 11, 28));
    }

    @Test
    void usesJapaneseDateAndMondayWeekBoundaries() {
        var parser = on("2026-11-29T15:01:00Z"); // Monday in Japan, Sunday in UTC.
        assertThat(parser.parse("今日")).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(parser.parse("来週月曜")).isEqualTo(LocalDate.of(2026, 12, 7));
        assertThat(parser.parse("今週日曜")).isEqualTo(LocalDate.of(2026, 12, 6));
        assertThat(parser.parse("再来週金曜")).isEqualTo(LocalDate.of(2026, 12, 18));
        assertThat(on("2026-11-29T00:00:00Z").parse("来週月曜")).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    void handlesYearRolloverAndLeapDays() {
        var parser = on("2026-12-31T00:00:00Z");
        assertThat(parser.parse("明日")).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(parser.parse("12月25日")).isEqualTo(LocalDate.of(2027, 12, 25));
        assertThat(parser.parse("12月31日")).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(parser.parse("2月29日")).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(on("2028-03-01T00:00:00Z").parse("1日前")).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void rejectsInvalidAndUnsupportedInputs() {
        var parser = on("2026-11-26T00:00:00Z");
        for (String text : new String[]{"", " ", "不明", "明日です", "2月30日", "13月1日", "2026年2月29日",
                "2026-02-30", "2026/1-2", "0000年1月1日", "999999999999999日後", "a".repeat(101)}) {
            assertThatThrownBy(() -> parser.parse(text)).as(text).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
