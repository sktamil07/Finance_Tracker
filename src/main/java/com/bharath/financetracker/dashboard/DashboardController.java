package com.bharath.financetracker.dashboard;

import com.bharath.financetracker.common.util.MonthUtils;
import com.bharath.financetracker.dashboard.dto.DashboardResponse;
import com.bharath.financetracker.dashboard.service.DashboardService;
import com.bharath.financetracker.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Metrics, allocation, category breakdown, and bond alerts for a month",
            description = "Everything is computed from the raw rows on each request; no derived value is stored.")
    @GetMapping
    public DashboardResponse dashboard(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Parameter(description = "YYYY-MM", example = "2026-04", required = true)
            @RequestParam String month) {
        return dashboardService.getDashboard(principal.id(), MonthUtils.parseYearMonth(month));
    }
}
