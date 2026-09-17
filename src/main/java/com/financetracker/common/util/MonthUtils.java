package com.financetracker.common.util;

import com.financetracker.common.exception.BadRequestException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Every {@code month} in this system is a {@link LocalDate} pinned to the first of the month.
 */
public final class MonthUtils {

    public static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private MonthUtils() {
    }

    /**
     * Parses a required {@code YYYY-MM} query parameter.
     */
    public static YearMonth parseYearMonth(String month) {
        if (month == null || month.isBlank()) {
            throw new BadRequestException("month is required and must be in YYYY-MM format");
        }
        try {
            return YearMonth.parse(month.trim(), MONTH_FORMAT);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid month '%s'; expected YYYY-MM".formatted(month));
        }
    }

    /**
     * Parses an optional {@code YYYY-MM} query parameter; null/blank means "no month filter".
     */
    public static YearMonth parseOptionalYearMonth(String month) {
        return month == null || month.isBlank() ? null : parseYearMonth(month);
    }

    /**
     * Parses a {@code YYYY-MM} query parameter into the first day of that month.
     */
    public static LocalDate parseMonth(String month) {
        return parseYearMonth(month).atDay(1);
    }

    /**
     * Normalises any date to the first of its month, so callers can never persist a stray day.
     */
    public static LocalDate firstOfMonth(LocalDate date) {
        return date == null ? null : date.withDayOfMonth(1);
    }

    public static LocalDate lastOfMonth(LocalDate anyDayInMonth) {
        return YearMonth.from(anyDayInMonth).atEndOfMonth();
    }

    public static String format(LocalDate monthStart) {
        return YearMonth.from(monthStart).format(MONTH_FORMAT);
    }

    public static LocalDate plusMonths(LocalDate monthStart, long months) {
        return monthStart.plusMonths(months).withDayOfMonth(1);
    }
}
