package com.financetracker.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Schema(name = "ExpensesByCategoryResponse")
public record ExpensesByCategoryResponse(
        @Schema(type = "string", example = "2026-04") YearMonth month,
        BigDecimal totalExpense,
        @Schema(description = "Biggest spend first") List<ExpenseCategoryGroup> categories
) {
}
