package com.financetracker.report;

import com.financetracker.common.util.MonthUtils;
import com.financetracker.report.dto.ReportSummaryResponse;
import com.financetracker.report.service.ReportService;
import com.financetracker.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reports")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Validated
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "Trailing N-month summary ending at the requested month",
            description = "Per-month invested/expenses/savings, the savings-rate trend, top expense "
                    + "categories across the window, and cumulative invested (portfolio growth).")
    @GetMapping("/summary")
    public ReportSummaryResponse summary(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Parameter(description = "YYYY-MM, the last month of the window", example = "2026-04", required = true)
            @RequestParam String month,
            @Parameter(description = "How many trailing months to include", example = "6")
            @RequestParam(defaultValue = "6") @Min(1) @Max(24) int months) {
        return reportService.summary(principal.id(), MonthUtils.parseYearMonth(month), months);
    }
}
