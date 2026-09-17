package com.financetracker.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.YearMonth;

@Schema(name = "SavingsRatePoint")
public record SavingsRatePoint(
        @Schema(type = "string", example = "2026-04") YearMonth month,
        BigDecimal savingsRate
) {
}
