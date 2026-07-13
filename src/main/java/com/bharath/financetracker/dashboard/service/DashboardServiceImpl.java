package com.bharath.financetracker.dashboard.service;

import com.bharath.financetracker.category.domain.Category;
import com.bharath.financetracker.common.util.MonthUtils;
import com.bharath.financetracker.common.util.MoneyMath;
import com.bharath.financetracker.dashboard.dto.AllocationResponse;
import com.bharath.financetracker.dashboard.dto.BondAlertResponse;
import com.bharath.financetracker.dashboard.dto.CategoryBreakdownItem;
import com.bharath.financetracker.dashboard.dto.DashboardMetrics;
import com.bharath.financetracker.dashboard.dto.DashboardResponse;
import com.bharath.financetracker.expense.domain.Expense;
import com.bharath.financetracker.expense.service.ExpenseService;
import com.bharath.financetracker.income.service.IncomeService;
import com.bharath.financetracker.investment.domain.Investment;
import com.bharath.financetracker.investment.service.InvestmentService;
import com.bharath.financetracker.user.domain.User;
import com.bharath.financetracker.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final IncomeService incomeService;
    private final InvestmentService investmentService;
    private final ExpenseService expenseService;
    private final UserService userService;

    @Override
    public DashboardResponse getDashboard(Long userId, YearMonth month) {
        User user = userService.requireUser(userId);

        List<Expense> expenses = expenseService.findForMonth(userId, month);
        List<Investment> investments = investmentService.findForMonth(userId, month);

        BigDecimal salary = incomeService.totalForMonth(userId, month);
        BigDecimal invested = sum(investments, Investment::getAmount);
        BigDecimal expenseTotal = sum(expenses, Expense::getAmount);
        BigDecimal savings = salary.subtract(invested).subtract(expenseTotal);

        DashboardMetrics metrics = new DashboardMetrics(
                salary,
                invested,
                expenseTotal,
                MoneyMath.money(savings),
                // savingsRate is 0 when salary is 0 — never a division by zero.
                MoneyMath.percentage(savings, salary));

        // Allocation is derived from the rupee amounts, never from a fixed 30/40/30 style split.
        AllocationResponse allocation = new AllocationResponse(
                MoneyMath.percentage(invested, salary),
                MoneyMath.percentage(expenseTotal, salary),
                MoneyMath.percentage(savings, salary));

        return new DashboardResponse(
                month,
                user.getCurrencyCode(),
                metrics,
                allocation,
                breakdown(expenses, e -> e.getCategory(), Expense::getAmount, expenseTotal),
                breakdown(investments, Investment::getCategory, Investment::getAmount, invested),
                bondAlerts(userId, month));
    }

    /**
     * Maturities landing in the requested month or the month after it.
     */
    private List<BondAlertResponse> bondAlerts(Long userId, YearMonth month) {
        LocalDate windowStart = month.atDay(1);
        LocalDate windowEnd = MonthUtils.lastOfMonth(month.plusMonths(1).atDay(1));

        return investmentService.findMaturingBetween(userId, windowStart, windowEnd).stream()
                .map(i -> new BondAlertResponse(
                        i.getId(),
                        i.getName(),
                        i.getCategory().getName(),
                        MoneyMath.money(i.getAmount()),
                        i.getMaturityDate(),
                        YearMonth.from(i.getMaturityDate()).equals(month),
                        i.getRoiNotes()))
                .toList();
    }

    private static <T> List<CategoryBreakdownItem> breakdown(List<T> rows,
                                                             Function<T, Category> categoryOf,
                                                             Function<T, BigDecimal> amountOf,
                                                             BigDecimal bucketTotal) {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        Map<Long, Category> categories = new LinkedHashMap<>();

        for (T row : rows) {
            Category category = categoryOf.apply(row);
            categories.putIfAbsent(category.getId(), category);
            totals.merge(category.getId(), amountOf.apply(row), BigDecimal::add);
        }

        return totals.entrySet().stream()
                .map(entry -> {
                    Category category = categories.get(entry.getKey());
                    return new CategoryBreakdownItem(
                            category.getId(),
                            category.getName(),
                            category.getIcon(),
                            MoneyMath.money(entry.getValue()),
                            MoneyMath.percentage(entry.getValue(), bucketTotal));
                })
                .sorted(Comparator.comparing(CategoryBreakdownItem::total).reversed())
                .toList();
    }

    private static <T> BigDecimal sum(List<T> rows, Function<T, BigDecimal> amountOf) {
        return MoneyMath.money(rows.stream().map(amountOf).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
