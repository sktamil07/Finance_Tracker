package com.financetracker.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "ExpenseRequest")
public record ExpenseRequest(

        @NotBlank @Size(max = 255) @Schema(example = "Weekly groceries") String description,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false, message = "amount must be greater than 0")
        @Digits(integer = 13, fraction = 2)
        @Schema(example = "2450.75")
        BigDecimal amount,

        @NotNull @Schema(description = "Must reference a category of type EXPENSE") Long categoryId,

        @NotNull @Schema(example = "2026-04-12") LocalDate transactionDate,

        @Size(max = 500) @Schema(nullable = true) String notes
) {
}
