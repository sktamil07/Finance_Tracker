package com.financetracker.dashboard.service;

import com.financetracker.dashboard.dto.DashboardResponse;

import java.time.YearMonth;

public interface DashboardService {

    /**
     * Everything on the dashboard is derived from the raw rows on every request. No percentage,
     * savings figure, or allocation split is ever read from storage.
     */
    DashboardResponse getDashboard(Long userId, YearMonth month);
}
