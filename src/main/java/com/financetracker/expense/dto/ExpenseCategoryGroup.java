package com.financetracker.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(name = "ExpenseCategoryGroup")
public record ExpenseCategoryGroup(
        Long categoryId,
        String categoryName,
        String categoryIcon,
        @Schema(description = "Total spend in this category for the month") BigDecimal total,
        @Schema(description = "Share of the month's total expense, 2dp; 0 when the month has no spend")
        BigDecimal percentageOfMonth,
        List<ExpenseResponse> transactions
) {
}
