package com.financetracker.dashboard;

import com.financetracker.category.domain.CategoryType;
import com.financetracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardIT extends AbstractIntegrationTest {

    /**
     * Salary 191,000; invested 130,000; expenses 41,098 => savings 19,902 and a 10.42% savings rate.
     */
    private TestUser seedAprilMonth() throws Exception {
        TestUser user = registerUser();

        long food = seededCategoryId(user, CategoryType.EXPENSE, "Food");
        long travel = seededCategoryId(user, CategoryType.EXPENSE, "Travel");
        long bonds = seededCategoryId(user, CategoryType.INVESTMENT, "Bonds");
        long fd = seededCategoryId(user, CategoryType.INVESTMENT, "FD");
        long stocks = seededCategoryId(user, CategoryType.INVESTMENT, "Stocks");

        create("/income", user, """
                {"month":"2026-04","amount":191000.00,"source":"Salary"}
                """);

        create("/investments", user, """
                {"name":"RBI Bond","amount":50000.00,"categoryId":%d,"month":"2026-04","maturityDate":"2026-04-28"}
                """.formatted(bonds));
        create("/investments", user, """
                {"name":"Bank FD","amount":45000.00,"categoryId":%d,"month":"2026-04","maturityDate":"2026-05-18"}
                """.formatted(fd));
        create("/investments", user, """
                {"name":"HDFC Shares","amount":35000.00,"categoryId":%d,"month":"2026-04"}
                """.formatted(stocks));

        create("/expenses", user, """
                {"description":"Groceries","amount":16000.00,"categoryId":%d,"transactionDate":"2026-04-03"}
                """.formatted(food));
        create("/expenses", user, """
                {"description":"Flight","amount":25098.00,"categoryId":%d,"transactionDate":"2026-04-09"}
                """.formatted(travel));

        return user;
    }

    @Test
    @DisplayName("metrics are computed from the raw rows at request time")
    void metricsHappyPath() throws Exception {
        TestUser user = seedAprilMonth();

        mockMvc.perform(authed(get(API + "/dashboard").param("month", "2026-04"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-04"))
                .andExpect(jsonPath("$.currencyCode").value("INR"))
                .andExpect(jsonPath("$.metrics.salary").value(191000.00))
                .andExpect(jsonPath("$.metrics.invested").value(130000.00))
                .andExpect(jsonPath("$.metrics.expenses").value(41098.00))
                .andExpect(jsonPath("$.metrics.savings").value(19902.00))
                .andExpect(jsonPath("$.metrics.savingsRate").value(10.42));
    }

    @Test
    @DisplayName("allocation is derived from the amounts and sums to 100%")
    void allocationHappyPath() throws Exception {
        TestUser user = seedAprilMonth();

        mockMvc.perform(authed(get(API + "/dashboard").param("month", "2026-04"), user))
                .andExpect(jsonPath("$.allocation.investedPercentage").value(68.06))
                .andExpect(jsonPath("$.allocation.expensesPercentage").value(21.52))
                .andExpect(jsonPath("$.allocation.savingsPercentage").value(10.42));
    }

    @Test
    @DisplayName("expense and investment breakdowns are sorted biggest first")
    void breakdownsHappyPath() throws Exception {
        TestUser user = seedAprilMonth();

        mockMvc.perform(authed(get(API + "/dashboard").param("month", "2026-04"), user))
                .andExpect(jsonPath("$.expenseBreakdown.length()").value(2))
                .andExpect(jsonPath("$.expenseBreakdown[0].categoryName").value("Travel"))
                .andExpect(jsonPath("$.expenseBreakdown[0].total").value(25098.00))
                .andExpect(jsonPath("$.expenseBreakdown[0].percentage").value(61.07))
                .andExpect(jsonPath("$.expenseBreakdown[1].categoryName").value("Food"))
                .andExpect(jsonPath("$.expenseBreakdown[1].percentage").value(38.93))

                .andExpect(jsonPath("$.investmentBreakdown.length()").value(3))
                .andExpect(jsonPath("$.investmentBreakdown[0].categoryName").value("Bonds"))
                .andExpect(jsonPath("$.investmentBreakdown[0].percentage").value(38.46))
                .andExpect(jsonPath("$.investmentBreakdown[1].categoryName").value("FD"))
                .andExpect(jsonPath("$.investmentBreakdown[1].percentage").value(34.62))
                .andExpect(jsonPath("$.investmentBreakdown[2].categoryName").value("Stocks"))
                .andExpect(jsonPath("$.investmentBreakdown[2].percentage").value(26.92));
    }

    @Test
    @DisplayName("bond alerts cover the requested month and the next one")
    void bondAlertsHappyPath() throws Exception {
        TestUser user = seedAprilMonth();

        mockMvc.perform(authed(get(API + "/dashboard").param("month", "2026-04"), user))
                .andExpect(jsonPath("$.bondAlerts.length()").value(2))
                .andExpect(jsonPath("$.bondAlerts[0].name").value("RBI Bond"))
                .andExpect(jsonPath("$.bondAlerts[0].maturityDate").value("2026-04-28"))
                .andExpect(jsonPath("$.bondAlerts[0].maturingThisMonth").value(true))
                .andExpect(jsonPath("$.bondAlerts[1].name").value("Bank FD"))
                .andExpect(jsonPath("$.bondAlerts[1].maturingThisMonth").value(false));

        // From March's perspective, only the April maturity is in the two-month window.
        mockMvc.perform(authed(get(API + "/dashboard").param("month", "2026-03"), user))
                .andExpect(jsonPath("$.bondAlerts.length()").value(1))
                .andExpect(jsonPath("$.bondAlerts[0].name").value("RBI Bond"))
                .andExpect(jsonPath("$.bondAlerts[0].maturingThisMonth").value(false));
    }

    @Test
    @DisplayName("a month with no salary reports a 0% savings rate, never a division by zero")
    void zeroSalaryMonth() throws Exception {
        TestUser user = seedAprilMonth();

        mockMvc.perform(authed(get(API + "/dashboard").param("month", "2026-01"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.salary").value(0.00))
                .andExpect(jsonPath("$.metrics.savings").value(0.00))
                .andExpect(jsonPath("$.metrics.savingsRate").value(0.00))
                .andExpect(jsonPath("$.allocation.savingsPercentage").value(0.00));
    }

    @Test
    @DisplayName("one user's dashboard never includes another user's rows")
    void dashboardIsScopedToTheAuthenticatedUser() throws Exception {
        seedAprilMonth();
        TestUser bob = registerUser();

        mockMvc.perform(authed(get(API + "/dashboard").param("month", "2026-04"), bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.salary").value(0.00))
                .andExpect(jsonPath("$.metrics.invested").value(0.00))
                .andExpect(jsonPath("$.metrics.expenses").value(0.00))
                .andExpect(jsonPath("$.expenseBreakdown.length()").value(0))
                .andExpect(jsonPath("$.bondAlerts.length()").value(0));
    }

    @Test
    void monthParameterIsRequiredAndValidated() throws Exception {
        TestUser user = registerUser();

        mockMvc.perform(authed(get(API + "/dashboard"), user))
                .andExpect(status().isBadRequest());

        mockMvc.perform(authed(get(API + "/dashboard").param("month", "2026-13"), user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("expected YYYY-MM")));
    }

    private void create(String path, TestUser user, String body) throws Exception {
        mockMvc.perform(authed(jsonBody(post(API + path), body), user)).andExpect(status().isCreated());
    }
}
