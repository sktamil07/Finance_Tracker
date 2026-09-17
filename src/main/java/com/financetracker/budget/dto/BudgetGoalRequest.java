package com.financetracker.budget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.YearMonth;

@Schema(name = "BudgetGoalRequest")
public record BudgetGoalRequest(

        @NotNull @Schema(description = "Must reference a category of type EXPENSE") Long categoryId,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false, message = "limitAmount must be greater than 0")
        @Digits(integer = 13, fraction = 2)
        @Schema(example = "15000.00")
        BigDecimal limitAmount,

        @Schema(type = "string", nullable = true, example = "2026-04",
                description = "Omit for a limit that recurs every month; set it to override one month only")
        YearMonth month
) {
}
