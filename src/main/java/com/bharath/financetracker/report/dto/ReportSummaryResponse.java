package com.bharath.financetracker.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.YearMonth;
import java.util.List;

@Schema(name = "ReportSummaryResponse", description = "Trailing N-month report ending at (and including) the requested month")
public record ReportSummaryResponse(
        @Schema(type = "string", example = "2025-11") YearMonth fromMonth,
        @Schema(type = "string", example = "2026-04") YearMonth toMonth,
        @Schema(example = "6") int months,
        @Schema(description = "Oldest month first") List<MonthlyPoint> monthly,
        List<SavingsRatePoint> savingsRateTrend,
        @Schema(description = "Biggest spend first") List<TopExpenseCategory> topExpenseCategories,
        ReportTotals totals
) {
}
