package com.bharath.financetracker.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Share of salary taken by each bucket, derived from the rupee amounts — never a fixed split.
 * All three are 0 when salary is 0.
 */
@Schema(name = "AllocationResponse")
public record AllocationResponse(
        BigDecimal investedPercentage,
        BigDecimal expensesPercentage,
        @Schema(description = "May be negative when the user overspent their salary") BigDecimal savingsPercentage
) {
}
