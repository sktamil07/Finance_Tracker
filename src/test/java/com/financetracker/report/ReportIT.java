package com.financetracker.report;

import com.financetracker.category.domain.CategoryType;
import com.financetracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportIT extends AbstractIntegrationTest {

    /**
     * March: salary 100,000, invested 20,000, expenses 30,000.
     * April: salary 100,000, invested 30,000, expenses 20,000.
     */
    private TestUser seedTwoMonths() throws Exception {
        TestUser user = registerUser();
        long food = seededCategoryId(user, CategoryType.EXPENSE, "Food");
        long travel = seededCategoryId(user, CategoryType.EXPENSE, "Travel");
        long stocks = seededCategoryId(user, CategoryType.INVESTMENT, "Stocks");

        create("/income", user, """
                {"month":"2026-03","amount":100000.00}""");
        create("/income", user, """
                {"month":"2026-04","amount":100000.00}""");

        create("/investments", user, """
                {"name":"Mar buy","amount":20000.00,"categoryId":%d,"month":"2026-03"}""".formatted(stocks));
        create("/investments", user, """
                {"name":"Apr buy","amount":30000.00,"categoryId":%d,"month":"2026-04"}""".formatted(stocks));

        create("/expenses", user, """
                {"description":"Mar food","amount":30000.00,"categoryId":%d,"transactionDate":"2026-03-15"}"""
                .formatted(food));
        create("/expenses", user, """
                {"description":"Apr travel","amount":20000.00,"categoryId":%d,"transactionDate":"2026-04-15"}"""
                .formatted(travel));

        return user;
    }

    @Test
    @DisplayName("a 6-month window ends at the requested month and starts 5 months earlier")
    void sixMonthWindow() throws Exception {
        TestUser user = seedTwoMonths();

        mockMvc.perform(authed(get(API + "/reports/summary")
                        .param("month", "2026-04").param("months", "6"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months").value(6))
                .andExpect(jsonPath("$.fromMonth").value("2025-11"))
                .andExpect(jsonPath("$.toMonth").value("2026-04"))
                .andExpect(jsonPath("$.monthly.length()").value(6))
                .andExpect(jsonPath("$.monthly[0].month").value("2025-11"))
                .andExpect(jsonPath("$.monthly[5].month").value("2026-04"))
                .andExpect(jsonPath("$.savingsRateTrend.length()").value(6));
    }

    @Test
    @DisplayName("months default to 6 when the parameter is omitted")
    void monthsDefaultsToSix() throws Exception {
        TestUser user = seedTwoMonths();

        mockMvc.perform(authed(get(API + "/reports/summary").param("month", "2026-04"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months").value(6));
    }

    @Test
    @DisplayName("per-month invested/expenses/savings and cumulative invested")
    void perMonthFiguresAndPortfolioGrowth() throws Exception {
        TestUser user = seedTwoMonths();

        mockMvc.perform(authed(get(API + "/reports/summary")
                        .param("month", "2026-04").param("months", "6"), user))
                // 2025-11 .. 2026-02 are empty months.
                .andExpect(jsonPath("$.monthly[0].salary").value(0.00))
                .andExpect(jsonPath("$.monthly[0].savingsRate").value(0.00))
                .andExpect(jsonPath("$.monthly[0].cumulativeInvested").value(0.00))

                // March: 100000 - 20000 - 30000 = 50000 saved => 50%
                .andExpect(jsonPath("$.monthly[4].month").value("2026-03"))
                .andExpect(jsonPath("$.monthly[4].invested").value(20000.00))
                .andExpect(jsonPath("$.monthly[4].expenses").value(30000.00))
                .andExpect(jsonPath("$.monthly[4].savings").value(50000.00))
                .andExpect(jsonPath("$.monthly[4].savingsRate").value(50.00))
                .andExpect(jsonPath("$.monthly[4].cumulativeInvested").value(20000.00))

                // April: 100000 - 30000 - 20000 = 50000 saved => 50%
                .andExpect(jsonPath("$.monthly[5].invested").value(30000.00))
                .andExpect(jsonPath("$.monthly[5].savings").value(50000.00))
                // Portfolio growth: 20000 + 30000.
                .andExpect(jsonPath("$.monthly[5].cumulativeInvested").value(50000.00));
    }

    @Test
    @DisplayName("top expense categories are shares of the window's total expense")
    void topExpenseCategoriesAcrossTheWindow() throws Exception {
        TestUser user = seedTwoMonths();

        mockMvc.perform(authed(get(API + "/reports/summary")
                        .param("month", "2026-04").param("months", "6"), user))
                .andExpect(jsonPath("$.topExpenseCategories.length()").value(2))
                // Food 30000 of 50000 = 60%, Travel 20000 of 50000 = 40%.
                .andExpect(jsonPath("$.topExpenseCategories[0].categoryName").value("Food"))
                .andExpect(jsonPath("$.topExpenseCategories[0].total").value(30000.00))
                .andExpect(jsonPath("$.topExpenseCategories[0].percentageOfWindow").value(60.00))
                .andExpect(jsonPath("$.topExpenseCategories[1].categoryName").value("Travel"))
                .andExpect(jsonPath("$.topExpenseCategories[1].percentageOfWindow").value(40.00));
    }

    @Test
    void windowTotals() throws Exception {
        TestUser user = seedTwoMonths();

        mockMvc.perform(authed(get(API + "/reports/summary")
                        .param("month", "2026-04").param("months", "6"), user))
                .andExpect(jsonPath("$.totals.salary").value(200000.00))
                .andExpect(jsonPath("$.totals.invested").value(50000.00))
                .andExpect(jsonPath("$.totals.expenses").value(50000.00))
                .andExpect(jsonPath("$.totals.savings").value(100000.00))
                .andExpect(jsonPath("$.totals.savingsRate").value(50.00));
    }

    @Test
    @DisplayName("months is bounded to 1..24")
    void monthsIsValidated() throws Exception {
        TestUser user = registerUser();

        mockMvc.perform(authed(get(API + "/reports/summary")
                        .param("month", "2026-04").param("months", "0"), user))
                .andExpect(status().isBadRequest());

        mockMvc.perform(authed(get(API + "/reports/summary")
                        .param("month", "2026-04").param("months", "25"), user))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("one user's report never includes another user's rows")
    void reportIsScopedToTheAuthenticatedUser() throws Exception {
        seedTwoMonths();
        TestUser bob = registerUser();

        mockMvc.perform(authed(get(API + "/reports/summary").param("month", "2026-04"), bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.salary").value(0.00))
                .andExpect(jsonPath("$.totals.invested").value(0.00))
                .andExpect(jsonPath("$.totals.expenses").value(0.00))
                .andExpect(jsonPath("$.topExpenseCategories.length()").value(0));
    }

    private void create(String path, TestUser user, String body) throws Exception {
        mockMvc.perform(authed(jsonBody(post(API + path), body), user)).andExpect(status().isCreated());
    }
}
