package com.bharath.financetracker.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.YearMonth;

@Schema(name = "BudgetStatusResponse", description = "Spend against the effective limit for one EXPENSE category")
public record BudgetStatusResponse(
        Long categoryId,
        String categoryName,
        String categoryIcon,
        @Schema(type = "string", example = "2026-04") YearMonth month,
        @Schema(description = "Total spend in this category for the month") BigDecimal spent,
        @Schema(description = "Effective limit: the month-specific goal if one exists, else the recurring goal")
        BigDecimal limitAmount,
        @Schema(description = "limitAmount - spent; negative once exceeded") BigDecimal remaining,
        @Schema(description = "spent / limitAmount * 100, 2dp") BigDecimal utilisationPercentage,
        BudgetStatus status,
        @Schema(description = "Id of the BudgetGoal row that supplied the limit") Long appliedGoalId,
        @Schema(description = "True when the effective limit came from the recurring goal") boolean recurringGoalApplied
) {
}
