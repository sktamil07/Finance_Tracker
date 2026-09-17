package com.financetracker.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.YearMonth;

@Schema(name = "MonthlyPoint")
public record MonthlyPoint(
        @Schema(type = "string", example = "2026-04") YearMonth month,
        BigDecimal salary,
        BigDecimal invested,
        BigDecimal expenses,
        BigDecimal savings,
        BigDecimal savingsRate,
        @Schema(description = "Running total of invested from the first month of the window: portfolio growth")
        BigDecimal cumulativeInvested
) {
}
