package com.financetracker.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Schema(name = "ExpenseResponse")
public record ExpenseResponse(
        Long id,
        String description,
        BigDecimal amount,
        Long categoryId,
        String categoryName,
        String categoryIcon,
        LocalDate transactionDate,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
