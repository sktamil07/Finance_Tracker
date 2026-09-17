package com.financetracker.report.service;

import com.financetracker.report.dto.ReportSummaryResponse;

import java.time.YearMonth;

public interface ReportService {

    /**
     * @param month  the last month of the window (inclusive)
     * @param months how many trailing months to include, e.g. 6
     */
    ReportSummaryResponse summary(Long userId, YearMonth month, int months);
}
