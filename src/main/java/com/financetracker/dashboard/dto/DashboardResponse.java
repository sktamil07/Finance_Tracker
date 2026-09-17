package com.financetracker.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.YearMonth;
import java.util.List;

@Schema(name = "DashboardResponse")
public record DashboardResponse(
        @Schema(type = "string", example = "2026-04") YearMonth month,
        @Schema(example = "INR") String currencyCode,
        DashboardMetrics metrics,
        AllocationResponse allocation,
        @Schema(description = "Expense totals per category, biggest first") List<CategoryBreakdownItem> expenseBreakdown,
        @Schema(description = "Investment totals per category, biggest first") List<CategoryBreakdownItem> investmentBreakdown,
        List<BondAlertResponse> bondAlerts
) {
}
