package com.bharath.financetracker.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;

@Schema(name = "BudgetGoalResponse")
public record BudgetGoalResponse(
        Long id,
        Long categoryId,
        String categoryName,
        BigDecimal limitAmount,
        @Schema(type = "string", nullable = true, example = "2026-04") YearMonth month,
        @Schema(description = "True when month is null, i.e. the limit applies every month") boolean recurring,
        Instant createdAt
) {
}
