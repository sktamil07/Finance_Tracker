package com.financetracker.budget.service;

import com.financetracker.budget.dto.BudgetGoalRequest;
import com.financetracker.budget.dto.BudgetGoalResponse;
import com.financetracker.budget.dto.BudgetStatusResponse;

import java.time.YearMonth;
import java.util.List;

public interface BudgetService {

    /**
     * Spend vs effective limit for every EXPENSE category that has a limit in this month.
     * The effective limit is the month-specific goal if one exists, otherwise the recurring goal.
     */
    List<BudgetStatusResponse> statusForMonth(Long userId, YearMonth month);

    /** The raw goal rows, so a client can find the id it needs for PUT/DELETE. */
    List<BudgetGoalResponse> listGoals(Long userId);

    BudgetGoalResponse create(Long userId, BudgetGoalRequest request);

    BudgetGoalResponse update(Long userId, Long goalId, BudgetGoalRequest request);

    void delete(Long userId, Long goalId);
}
