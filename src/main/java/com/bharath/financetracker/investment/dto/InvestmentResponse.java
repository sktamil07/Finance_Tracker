package com.bharath.financetracker.investment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

@Schema(name = "InvestmentResponse")
public record InvestmentResponse(
        Long id,
        String name,
        BigDecimal amount,
        Long categoryId,
        String categoryName,
        String categoryIcon,
        @Schema(type = "string", example = "2026-04") YearMonth month,
        String roiNotes,
        boolean recurring,
        Integer recurringDayOfMonth,
        LocalDate maturityDate,
        Instant createdAt,
        Instant updatedAt
) {
}
