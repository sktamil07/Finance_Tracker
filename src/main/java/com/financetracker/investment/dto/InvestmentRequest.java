package com.financetracker.investment.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Schema(name = "InvestmentRequest")
public record InvestmentRequest(

        @NotBlank @Size(max = 120) @Schema(example = "Nifty 50 Index Fund") String name,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false, message = "amount must be greater than 0")
        @Digits(integer = 13, fraction = 2)
        @Schema(example = "25000.00")
        BigDecimal amount,

        @NotNull @Schema(description = "Must reference a category of type INVESTMENT") Long categoryId,

        @NotNull @Schema(type = "string", example = "2026-04") YearMonth month,

        @Size(max = 5000)
        @Schema(description = "Free text: ROI %, SIP date, or maturity note", example = "SIP on the 5th; 12.4% XIRR")
        String roiNotes,

        @Schema(defaultValue = "false") boolean recurring,

        @Min(1) @Max(31) @Schema(nullable = true, example = "5") Integer recurringDayOfMonth,

        @Schema(nullable = true, example = "2026-05-18") LocalDate maturityDate
) {

    @JsonIgnore
    @AssertTrue(message = "recurringDayOfMonth is required when recurring is true")
    public boolean isRecurringDayConsistent() {
        return !recurring || recurringDayOfMonth != null;
    }
}
