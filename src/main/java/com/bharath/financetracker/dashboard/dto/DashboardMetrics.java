package com.bharath.financetracker.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "DashboardMetrics", description = "All values computed at request time; nothing derived is stored")
public record DashboardMetrics(
        @Schema(description = "Sum of income rows for the month") BigDecimal salary,
        @Schema(description = "Sum of investment rows for the month") BigDecimal invested,
        @Schema(description = "Sum of expense rows dated inside the month") BigDecimal expenses,
        @Schema(description = "salary - invested - expenses; may be negative") BigDecimal savings,
        @Schema(description = "savings / salary * 100, 2dp. Exactly 0 when salary is 0") BigDecimal savingsRate
) {
}
