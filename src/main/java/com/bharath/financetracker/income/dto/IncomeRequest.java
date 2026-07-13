package com.bharath.financetracker.income.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.YearMonth;

@Schema(name = "IncomeRequest")
public record IncomeRequest(

        @NotNull @Schema(type = "string", example = "2026-04") YearMonth month,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false, message = "amount must be greater than 0")
        @Digits(integer = 13, fraction = 2)
        @Schema(example = "191000.00")
        BigDecimal amount,

        @Size(max = 100) @Schema(example = "Salary", defaultValue = "Salary") String source
) {
}
