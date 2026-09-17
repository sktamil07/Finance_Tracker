package com.financetracker.income.service;

import com.financetracker.income.dto.IncomeRequest;
import com.financetracker.income.dto.IncomeResponse;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public interface IncomeService {

    /** @param month null lists every income row the user owns, newest month first. */
    List<IncomeResponse> list(Long userId, YearMonth month);

    IncomeResponse create(Long userId, IncomeRequest request);

    IncomeResponse update(Long userId, Long incomeId, IncomeRequest request);

    void delete(Long userId, Long incomeId);

    /** Sum of every income row booked for the month. Zero when there are none. */
    BigDecimal totalForMonth(Long userId, YearMonth month);
}
