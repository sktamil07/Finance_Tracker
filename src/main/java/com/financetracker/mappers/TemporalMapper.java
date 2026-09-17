package com.financetracker.mappers;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Months live as a first-of-month {@link LocalDate} in the database and as a {@link YearMonth}
 * ("2026-04") on the wire. MapStruct picks these up automatically for any mapper that uses
 * {@code uses = TemporalMapper.class}.
 */
@Component
public class TemporalMapper {

    public YearMonth toYearMonth(LocalDate date) {
        return date == null ? null : YearMonth.from(date);
    }

    public LocalDate toFirstOfMonth(YearMonth month) {
        return month == null ? null : month.atDay(1);
    }
}
