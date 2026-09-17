package com.financetracker.report.service;

import com.financetracker.common.util.MoneyMath;
import com.financetracker.expense.repository.CategoryTotal;
import com.financetracker.expense.service.ExpenseService;
import com.financetracker.income.service.IncomeService;
import com.financetracker.investment.service.InvestmentService;
import com.financetracker.report.dto.MonthlyPoint;
import com.financetracker.report.dto.ReportSummaryResponse;
import com.financetracker.report.dto.ReportTotals;
import com.financetracker.report.dto.SavingsRatePoint;
import com.financetracker.report.dto.TopExpenseCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final IncomeService incomeService;
    private final InvestmentService investmentService;
    private final ExpenseService expenseService;

    @Override
    public ReportSummaryResponse summary(Long userId, YearMonth month, int months) {
        YearMonth from = month.minusMonths(months - 1L);

        List<MonthlyPoint> monthly = new ArrayList<>(months);
        List<SavingsRatePoint> trend = new ArrayList<>(months);

        BigDecimal cumulativeInvested = BigDecimal.ZERO;
        BigDecimal totalSalary = BigDecimal.ZERO;
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        // One month at a time: three indexed lookups per month, ~18 cheap queries for a 6-month
        // window. Clearer than a single grouped query, and the window is bounded to 24 months.
        for (int i = 0; i < months; i++) {
            YearMonth current = from.plusMonths(i);

            BigDecimal salary = incomeService.totalForMonth(userId, current);
            BigDecimal invested = investmentService.totalForMonth(userId, current);
            BigDecimal expenses = expenseService.totalForMonth(userId, current);
            BigDecimal savings = MoneyMath.money(salary.subtract(invested).subtract(expenses));
            BigDecimal savingsRate = MoneyMath.percentage(savings, salary);

            cumulativeInvested = cumulativeInvested.add(invested);
            totalSalary = totalSalary.add(salary);
            totalInvested = totalInvested.add(invested);
            totalExpenses = totalExpenses.add(expenses);

            monthly.add(new MonthlyPoint(current, salary, invested, expenses, savings, savingsRate,
                    MoneyMath.money(cumulativeInvested)));
            trend.add(new SavingsRatePoint(current, savingsRate));
        }

        BigDecimal totalSavings = MoneyMath.money(totalSalary.subtract(totalInvested).subtract(totalExpenses));

        ReportTotals totals = new ReportTotals(
                MoneyMath.money(totalSalary),
                MoneyMath.money(totalInvested),
                MoneyMath.money(totalExpenses),
                totalSavings,
                MoneyMath.percentage(totalSavings, totalSalary));

        return new ReportSummaryResponse(from, month, months, monthly, trend,
                topCategories(userId, from, month, totalExpenses), totals);
    }

    private List<TopExpenseCategory> topCategories(Long userId, YearMonth from, YearMonth to,
                                                   BigDecimal windowExpenseTotal) {
        List<CategoryTotal> totals = expenseService.totalsByCategory(userId, from.atDay(1), to.atEndOfMonth());
        return totals.stream()
                .map(row -> new TopExpenseCategory(
                        row.categoryId(),
                        row.categoryName(),
                        row.categoryIcon(),
                        MoneyMath.money(row.total()),
                        MoneyMath.percentage(row.total(), windowExpenseTotal)))
                .toList();
    }
}
