package com.bharath.financetracker.dashboard.service;

import com.bharath.financetracker.category.domain.Category;
import com.bharath.financetracker.category.domain.CategoryType;
import com.bharath.financetracker.dashboard.dto.BondAlertResponse;
import com.bharath.financetracker.dashboard.dto.CategoryBreakdownItem;
import com.bharath.financetracker.dashboard.dto.DashboardResponse;
import com.bharath.financetracker.expense.domain.Expense;
import com.bharath.financetracker.expense.service.ExpenseService;
import com.bharath.financetracker.income.service.IncomeService;
import com.bharath.financetracker.investment.domain.Investment;
import com.bharath.financetracker.investment.service.InvestmentService;
import com.bharath.financetracker.user.domain.User;
import com.bharath.financetracker.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final YearMonth APRIL_2026 = YearMonth.of(2026, 4);

    @Mock
    private IncomeService incomeService;
    @Mock
    private InvestmentService investmentService;
    @Mock
    private ExpenseService expenseService;
    @Mock
    private UserService userService;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Category food;
    private Category travel;
    private Category bonds;
    private Category fd;

    @BeforeEach
    void setUp() {
        food = category(1L, "Food", "utensils", CategoryType.EXPENSE);
        travel = category(2L, "Travel", "plane", CategoryType.EXPENSE);
        bonds = category(10L, "Bonds", "landmark", CategoryType.INVESTMENT);
        fd = category(11L, "FD", "piggy-bank", CategoryType.INVESTMENT);

        when(userService.requireUser(USER_ID)).thenReturn(User.builder().id(USER_ID).currencyCode("INR").build());
    }

    @Test
    @DisplayName("savings = salary - invested - expenses, and savingsRate = savings / salary * 100")
    void computesMetricsFromRawRows() {
        stubMonth(
                new BigDecimal("191000.00"),
                List.of(investment(1L, "RBI Bond", "50000.00", bonds, null),
                        investment(2L, "Bank FD", "40000.00", fd, null),
                        investment(3L, "Index Fund", "40000.00", fd, null)),
                List.of(expense(1L, "Groceries", "16000.00", food),
                        expense(2L, "Flight", "25098.00", travel)));

        DashboardResponse dashboard = dashboardService.getDashboard(USER_ID, APRIL_2026);

        assertThat(dashboard.month()).isEqualTo(APRIL_2026);
        assertThat(dashboard.currencyCode()).isEqualTo("INR");
        assertThat(dashboard.metrics().salary()).isEqualByComparingTo("191000.00");
        assertThat(dashboard.metrics().invested()).isEqualByComparingTo("130000.00");
        assertThat(dashboard.metrics().expenses()).isEqualByComparingTo("41098.00");
        assertThat(dashboard.metrics().savings()).isEqualByComparingTo("19902.00");
        // 19902 / 191000 * 100 = 10.4198...
        assertThat(dashboard.metrics().savingsRate()).isEqualByComparingTo("10.42");
    }

    @Test
    @DisplayName("allocation is derived from the rupee amounts and sums to 100%")
    void allocationIsDerivedFromAmounts() {
        stubMonth(
                new BigDecimal("191000.00"),
                List.of(investment(1L, "RBI Bond", "130000.00", bonds, null)),
                List.of(expense(1L, "Groceries", "41098.00", food)));

        var allocation = dashboardService.getDashboard(USER_ID, APRIL_2026).allocation();

        assertThat(allocation.investedPercentage()).isEqualByComparingTo("68.06");
        assertThat(allocation.expensesPercentage()).isEqualByComparingTo("21.52");
        assertThat(allocation.savingsPercentage()).isEqualByComparingTo("10.42");

        BigDecimal sum = allocation.investedPercentage()
                .add(allocation.expensesPercentage())
                .add(allocation.savingsPercentage());
        assertThat(sum).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("a zero salary yields a 0% savings rate and a 0% allocation, never a division by zero")
    void zeroSalaryNeverDividesByZero() {
        stubMonth(BigDecimal.ZERO, List.of(), List.of(expense(1L, "Groceries", "5000.00", food)));

        DashboardResponse dashboard = dashboardService.getDashboard(USER_ID, APRIL_2026);

        assertThat(dashboard.metrics().savings()).isEqualByComparingTo("-5000.00");
        assertThat(dashboard.metrics().savingsRate()).isEqualByComparingTo("0.00");
        assertThat(dashboard.allocation().investedPercentage()).isEqualByComparingTo("0.00");
        assertThat(dashboard.allocation().expensesPercentage()).isEqualByComparingTo("0.00");
        assertThat(dashboard.allocation().savingsPercentage()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("overspending the salary produces negative savings and a negative rate")
    void overspendingProducesNegativeSavings() {
        stubMonth(new BigDecimal("50000.00"), List.of(), List.of(expense(1L, "Rent", "60000.00", food)));

        var metrics = dashboardService.getDashboard(USER_ID, APRIL_2026).metrics();

        assertThat(metrics.savings()).isEqualByComparingTo("-10000.00");
        assertThat(metrics.savingsRate()).isEqualByComparingTo("-20.00");
    }

    @Test
    @DisplayName("expenses are grouped by category, biggest first, with each share of the month total")
    void expenseBreakdownIsSortedAndPercentaged() {
        stubMonth(
                new BigDecimal("100000.00"),
                List.of(),
                List.of(expense(1L, "Groceries", "12000.00", food),
                        expense(2L, "Dining out", "4000.00", food),
                        expense(3L, "Flight", "24000.00", travel)));

        List<CategoryBreakdownItem> breakdown =
                dashboardService.getDashboard(USER_ID, APRIL_2026).expenseBreakdown();

        assertThat(breakdown).hasSize(2);
        // Travel (24000) outranks Food (12000 + 4000 = 16000).
        assertThat(breakdown.get(0).categoryName()).isEqualTo("Travel");
        assertThat(breakdown.get(0).total()).isEqualByComparingTo("24000.00");
        assertThat(breakdown.get(0).percentage()).isEqualByComparingTo("60.00");

        assertThat(breakdown.get(1).categoryName()).isEqualTo("Food");
        assertThat(breakdown.get(1).total()).isEqualByComparingTo("16000.00");
        assertThat(breakdown.get(1).percentage()).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("a month with no expenses yields 0% shares rather than an arithmetic error")
    void emptyMonthIsSafe() {
        stubMonth(BigDecimal.ZERO, List.of(), List.of());

        DashboardResponse dashboard = dashboardService.getDashboard(USER_ID, APRIL_2026);

        assertThat(dashboard.metrics().savings()).isEqualByComparingTo("0.00");
        assertThat(dashboard.metrics().savingsRate()).isEqualByComparingTo("0.00");
        assertThat(dashboard.expenseBreakdown()).isEmpty();
        assertThat(dashboard.investmentBreakdown()).isEmpty();
        assertThat(dashboard.bondAlerts()).isEmpty();
    }

    @Test
    @DisplayName("bond alerts span the requested month and the next, flagging which land this month")
    void bondAlertsCoverThisMonthAndNext() {
        when(incomeService.totalForMonth(USER_ID, APRIL_2026)).thenReturn(BigDecimal.ZERO);
        when(expenseService.findForMonth(USER_ID, APRIL_2026)).thenReturn(List.of());
        when(investmentService.findForMonth(USER_ID, APRIL_2026)).thenReturn(List.of());
        when(investmentService.findMaturingBetween(eq(USER_ID), any(), any())).thenReturn(List.of(
                investment(1L, "Bank FD", "40000.00", fd, LocalDate.of(2026, 4, 28)),
                investment(2L, "RBI Bond", "50000.00", bonds, LocalDate.of(2026, 5, 18))));

        List<BondAlertResponse> alerts = dashboardService.getDashboard(USER_ID, APRIL_2026).bondAlerts();

        ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> to = ArgumentCaptor.forClass(LocalDate.class);
        verify(investmentService).findMaturingBetween(eq(USER_ID), from.capture(), to.capture());

        assertThat(from.getValue()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(to.getValue()).isEqualTo(LocalDate.of(2026, 5, 31));

        assertThat(alerts).hasSize(2);
        assertThat(alerts.get(0).name()).isEqualTo("Bank FD");
        assertThat(alerts.get(0).maturingThisMonth()).isTrue();
        assertThat(alerts.get(1).name()).isEqualTo("RBI Bond");
        assertThat(alerts.get(1).maturingThisMonth()).isFalse();
    }

    private void stubMonth(BigDecimal salary, List<Investment> investments, List<Expense> expenses) {
        when(incomeService.totalForMonth(USER_ID, APRIL_2026)).thenReturn(salary);
        when(investmentService.findForMonth(USER_ID, APRIL_2026)).thenReturn(investments);
        when(expenseService.findForMonth(USER_ID, APRIL_2026)).thenReturn(expenses);
        when(investmentService.findMaturingBetween(eq(USER_ID), any(), any())).thenReturn(List.of());
    }

    private static Category category(Long id, String name, String icon, CategoryType type) {
        return Category.builder().id(id).name(name).icon(icon).type(type).build();
    }

    private static Expense expense(Long id, String description, String amount, Category category) {
        return Expense.builder()
                .id(id)
                .description(description)
                .amount(new BigDecimal(amount))
                .category(category)
                .transactionDate(APRIL_2026.atDay(1))
                .build();
    }

    private static Investment investment(Long id, String name, String amount, Category category,
                                         LocalDate maturityDate) {
        return Investment.builder()
                .id(id)
                .name(name)
                .amount(new BigDecimal(amount))
                .category(category)
                .month(APRIL_2026.atDay(1))
                .maturityDate(maturityDate)
                .build();
    }
}
