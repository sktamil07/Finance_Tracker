package com.bharath.financetracker.report.service;

import com.bharath.financetracker.expense.repository.CategoryTotal;
import com.bharath.financetracker.expense.service.ExpenseService;
import com.bharath.financetracker.income.service.IncomeService;
import com.bharath.financetracker.investment.service.InvestmentService;
import com.bharath.financetracker.report.dto.MonthlyPoint;
import com.bharath.financetracker.report.dto.ReportSummaryResponse;
import com.bharath.financetracker.report.dto.TopExpenseCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final YearMonth APRIL_2026 = YearMonth.of(2026, 4);

    @Mock
    private IncomeService incomeService;
    @Mock
    private InvestmentService investmentService;
    @Mock
    private ExpenseService expenseService;

    @InjectMocks
    private ReportServiceImpl reportService;

    /**
     * A six-month window ending April 2026, deliberately including a zero-salary month (Jan) and a
     * break-even month (Mar) so the rate maths is exercised at its edges.
     */
    private void stubSixMonths() {
        stubMonth(YearMonth.of(2025, 11), "100000.00", "10000.00", "40000.00");
        stubMonth(YearMonth.of(2025, 12), "100000.00", "20000.00", "30000.00");
        stubMonth(YearMonth.of(2026, 1), "0.00", "0.00", "10000.00");
        stubMonth(YearMonth.of(2026, 2), "120000.00", "30000.00", "60000.00");
        stubMonth(YearMonth.of(2026, 3), "120000.00", "0.00", "120000.00");
        stubMonth(YearMonth.of(2026, 4), "191000.00", "130000.00", "41098.00");

        lenient().when(expenseService.totalsByCategory(anyLong(), any(), any())).thenReturn(List.of(
                new CategoryTotal(1L, "Food", "utensils", new BigDecimal("150000.00")),
                new CategoryTotal(2L, "Travel", "plane", new BigDecimal("100000.00")),
                new CategoryTotal(3L, "Other", "ellipsis", new BigDecimal("51098.00"))));
    }

    @Test
    @DisplayName("the window is the trailing N months ending at (and including) the requested month")
    void windowSpansTrailingMonths() {
        stubSixMonths();

        ReportSummaryResponse report = reportService.summary(USER_ID, APRIL_2026, 6);

        assertThat(report.months()).isEqualTo(6);
        assertThat(report.fromMonth()).isEqualTo(YearMonth.of(2025, 11));
        assertThat(report.toMonth()).isEqualTo(APRIL_2026);
        assertThat(report.monthly()).extracting(MonthlyPoint::month).containsExactly(
                YearMonth.of(2025, 11), YearMonth.of(2025, 12), YearMonth.of(2026, 1),
                YearMonth.of(2026, 2), YearMonth.of(2026, 3), APRIL_2026);
    }

    @Test
    @DisplayName("per-month savings and savings rate, with a zero-salary month reported as 0%")
    void perMonthSavingsAndRate() {
        stubSixMonths();

        List<MonthlyPoint> monthly = reportService.summary(USER_ID, APRIL_2026, 6).monthly();

        assertThat(monthly.get(0).savings()).isEqualByComparingTo("50000.00");
        assertThat(monthly.get(0).savingsRate()).isEqualByComparingTo("50.00");

        // Zero salary: savings is negative but the rate must be exactly 0, never a division by zero.
        assertThat(monthly.get(2).savings()).isEqualByComparingTo("-10000.00");
        assertThat(monthly.get(2).savingsRate()).isEqualByComparingTo("0.00");

        // Break-even month.
        assertThat(monthly.get(4).savings()).isEqualByComparingTo("0.00");
        assertThat(monthly.get(4).savingsRate()).isEqualByComparingTo("0.00");

        assertThat(monthly.get(5).savings()).isEqualByComparingTo("19902.00");
        assertThat(monthly.get(5).savingsRate()).isEqualByComparingTo("10.42");
    }

    @Test
    @DisplayName("cumulative invested is a running total: portfolio growth across the window")
    void cumulativeInvestedIsARunningTotal() {
        stubSixMonths();

        ReportSummaryResponse report = reportService.summary(USER_ID, APRIL_2026, 6);

        assertThat(report.monthly()).extracting(MonthlyPoint::cumulativeInvested)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(
                        new BigDecimal("10000.00"),
                        new BigDecimal("30000.00"),
                        new BigDecimal("30000.00"),
                        new BigDecimal("60000.00"),
                        new BigDecimal("60000.00"),
                        new BigDecimal("190000.00"));

        // The final running total must agree with the window's invested total.
        assertThat(report.monthly().get(5).cumulativeInvested())
                .isEqualByComparingTo(report.totals().invested());
    }

    @Test
    @DisplayName("the savings-rate trend mirrors the per-month rates")
    void savingsRateTrendMirrorsMonthlyRates() {
        stubSixMonths();

        ReportSummaryResponse report = reportService.summary(USER_ID, APRIL_2026, 6);

        assertThat(report.savingsRateTrend()).hasSize(6);
        for (int i = 0; i < 6; i++) {
            assertThat(report.savingsRateTrend().get(i).month()).isEqualTo(report.monthly().get(i).month());
            assertThat(report.savingsRateTrend().get(i).savingsRate())
                    .isEqualByComparingTo(report.monthly().get(i).savingsRate());
        }
    }

    @Test
    @DisplayName("window totals aggregate every month, and the window rate is computed from them")
    void windowTotals() {
        stubSixMonths();

        var totals = reportService.summary(USER_ID, APRIL_2026, 6).totals();

        assertThat(totals.salary()).isEqualByComparingTo("631000.00");
        assertThat(totals.invested()).isEqualByComparingTo("190000.00");
        assertThat(totals.expenses()).isEqualByComparingTo("301098.00");
        assertThat(totals.savings()).isEqualByComparingTo("139902.00");
        // 139902 / 631000 * 100 = 22.1714...
        assertThat(totals.savingsRate()).isEqualByComparingTo("22.17");
    }

    @Test
    @DisplayName("top expense categories are shares of the window's total expense and sum to 100%")
    void topExpenseCategoriesArePercentagedAcrossTheWindow() {
        stubSixMonths();

        List<TopExpenseCategory> top = reportService.summary(USER_ID, APRIL_2026, 6).topExpenseCategories();

        assertThat(top).extracting(TopExpenseCategory::categoryName)
                .containsExactly("Food", "Travel", "Other");
        assertThat(top.get(0).percentageOfWindow()).isEqualByComparingTo("49.82");
        assertThat(top.get(1).percentageOfWindow()).isEqualByComparingTo("33.21");
        assertThat(top.get(2).percentageOfWindow()).isEqualByComparingTo("16.97");

        BigDecimal sum = top.stream()
                .map(TopExpenseCategory::percentageOfWindow)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("the category window spans the first day of the first month to the last day of the last")
    void topCategoryWindowCoversWholeRange() {
        stubSixMonths();

        reportService.summary(USER_ID, APRIL_2026, 6);

        org.mockito.Mockito.verify(expenseService)
                .totalsByCategory(USER_ID, LocalDate.of(2025, 11, 1), LocalDate.of(2026, 4, 30));
    }

    @Test
    @DisplayName("months=1 reports the requested month alone")
    void singleMonthWindow() {
        stubMonth(APRIL_2026, "191000.00", "130000.00", "41098.00");
        lenient().when(expenseService.totalsByCategory(anyLong(), any(), any())).thenReturn(List.of());

        ReportSummaryResponse report = reportService.summary(USER_ID, APRIL_2026, 1);

        assertThat(report.fromMonth()).isEqualTo(APRIL_2026);
        assertThat(report.toMonth()).isEqualTo(APRIL_2026);
        assertThat(report.monthly()).hasSize(1);
        assertThat(report.monthly().get(0).cumulativeInvested()).isEqualByComparingTo("130000.00");
    }

    private void stubMonth(YearMonth month, String salary, String invested, String expenses) {
        lenient().when(incomeService.totalForMonth(USER_ID, month)).thenReturn(new BigDecimal(salary));
        lenient().when(investmentService.totalForMonth(USER_ID, month)).thenReturn(new BigDecimal(invested));
        lenient().when(expenseService.totalForMonth(USER_ID, month)).thenReturn(new BigDecimal(expenses));
    }
}
