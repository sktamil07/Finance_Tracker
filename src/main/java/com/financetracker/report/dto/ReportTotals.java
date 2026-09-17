package com.financetracker.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "ReportTotals", description = "Window-wide totals")
public record ReportTotals(
        BigDecimal salary,
        BigDecimal invested,
        BigDecimal expenses,
        BigDecimal savings,
        @Schema(description = "Window savings / window salary * 100; 0 when salary is 0") BigDecimal savingsRate
) {
}
