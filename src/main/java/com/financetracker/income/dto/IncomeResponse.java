package com.financetracker.income.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;

@Schema(name = "IncomeResponse")
public record IncomeResponse(
        Long id,
        @Schema(type = "string", example = "2026-04") YearMonth month,
        BigDecimal amount,
        String source,
        Instant createdAt
) {
}
