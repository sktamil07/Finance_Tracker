package com.financetracker.budget;

import com.financetracker.category.domain.CategoryType;
import com.financetracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BudgetIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("create a recurring goal, then a month-specific override that wins for that month")
    void monthSpecificGoalOverridesTheRecurringOne() throws Exception {
        TestUser user = registerUser();
        long food = seededCategoryId(user, CategoryType.EXPENSE, "Food");

        // Recurring: no month.
        mockMvc.perform(authed(jsonBody(post(API + "/budgets"), """
                        {"categoryId":%d,"limitAmount":18000.00}
                        """.formatted(food)), user))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recurring").value(true))
                .andExpect(jsonPath("$.month").doesNotExist());

        // April-only override.
        mockMvc.perform(authed(jsonBody(post(API + "/budgets"), """
                        {"categoryId":%d,"limitAmount":5000.00,"month":"2026-04"}
                        """.formatted(food)), user))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recurring").value(false))
                .andExpect(jsonPath("$.month").value("2026-04"));

        // April uses the override.
        mockMvc.perform(authed(get(API + "/budgets").param("month", "2026-04"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].limitAmount").value(5000.00))
                .andExpect(jsonPath("$[0].recurringGoalApplied").value(false));

        // May falls back to the recurring goal.
        mockMvc.perform(authed(get(API + "/budgets").param("month", "2026-05"), user))
                .andExpect(jsonPath("$[0].limitAmount").value(18000.00))
                .andExpect(jsonPath("$[0].recurringGoalApplied").value(true));
    }

    @Test
    @DisplayName("status crosses NONE -> WARNING -> EXCEEDED as spend climbs against the limit")
    void statusTracksSpendAgainstTheLimit() throws Exception {
        TestUser user = registerUser();
        long food = seededCategoryId(user, CategoryType.EXPENSE, "Food");

        mockMvc.perform(authed(jsonBody(post(API + "/budgets"), """
                {"categoryId":%d,"limitAmount":10000.00,"month":"2026-04"}
                """.formatted(food)), user)).andExpect(status().isCreated());

        // Nothing spent yet.
        mockMvc.perform(authed(get(API + "/budgets").param("month", "2026-04"), user))
                .andExpect(jsonPath("$[0].spent").value(0.00))
                .andExpect(jsonPath("$[0].status").value("NONE"))
                .andExpect(jsonPath("$[0].remaining").value(10000.00));

        // 7,999.99 -> still under 80%.
        createExpense(user, "Groceries", "7999.99", food);
        mockMvc.perform(authed(get(API + "/budgets").param("month", "2026-04"), user))
                .andExpect(jsonPath("$[0].status").value("NONE"));

        // Nudge to exactly 8,000.00 -> 80% -> WARNING.
        createExpense(user, "Sweets", "0.01", food);
        mockMvc.perform(authed(get(API + "/budgets").param("month", "2026-04"), user))
                .andExpect(jsonPath("$[0].spent").value(8000.00))
                .andExpect(jsonPath("$[0].utilisationPercentage").value(80.00))
                .andExpect(jsonPath("$[0].status").value("WARNING"));

        // Exactly at the limit is still WARNING.
        createExpense(user, "Dinner", "2000.00", food);
        mockMvc.perform(authed(get(API + "/budgets").param("month", "2026-04"), user))
                .andExpect(jsonPath("$[0].spent").value(10000.00))
                .andExpect(jsonPath("$[0].status").value("WARNING"))
                .andExpect(jsonPath("$[0].remaining").value(0.00));

        // One paisa over -> EXCEEDED, with negative remaining.
        createExpense(user, "Gum", "0.01", food);
        mockMvc.perform(authed(get(API + "/budgets").param("month", "2026-04"), user))
                .andExpect(jsonPath("$[0].status").value("EXCEEDED"))
                .andExpect(jsonPath("$[0].remaining").value(-0.01));
    }

    @Test
    @DisplayName("only spend inside the month counts towards that month's budget")
    void spendIsScopedToTheMonth() throws Exception {
        TestUser user = registerUser();
        long food = seededCategoryId(user, CategoryType.EXPENSE, "Food");

        mockMvc.perform(authed(jsonBody(post(API + "/budgets"), """
                {"categoryId":%d,"limitAmount":10000.00}
                """.formatted(food)), user)).andExpect(status().isCreated());

        createExpenseOn(user, "March spend", "9000.00", food, "2026-03-31");
        createExpenseOn(user, "April spend", "1000.00", food, "2026-04-01");

        mockMvc.perform(authed(get(API + "/budgets").param("month", "2026-04"), user))
                .andExpect(jsonPath("$[0].spent").value(1000.00));
        mockMvc.perform(authed(get(API + "/budgets").param("month", "2026-03"), user))
                .andExpect(jsonPath("$[0].spent").value(9000.00));
    }

    @Test
    @DisplayName("a second recurring goal for the same category is a 409")
    void duplicateRecurringGoalIsConflict() throws Exception {
        TestUser user = registerUser();
        long food = seededCategoryId(user, CategoryType.EXPENSE, "Food");

        String body = """
                {"categoryId":%d,"limitAmount":18000.00}
                """.formatted(food);

        mockMvc.perform(authed(jsonBody(post(API + "/budgets"), body), user)).andExpect(status().isCreated());
        mockMvc.perform(authed(jsonBody(post(API + "/budgets"), body), user))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A recurring budget goal already exists for this category"));

        // A second month-specific goal for the same month is likewise a conflict.
        String april = """
                {"categoryId":%d,"limitAmount":5000.00,"month":"2026-04"}
                """.formatted(food);
        mockMvc.perform(authed(jsonBody(post(API + "/budgets"), april), user)).andExpect(status().isCreated());
        mockMvc.perform(authed(jsonBody(post(API + "/budgets"), april), user)).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("an INVESTMENT category cannot carry a budget goal")
    void categoryTypeMustBeExpense() throws Exception {
        TestUser user = registerUser();
        long stocks = seededCategoryId(user, CategoryType.INVESTMENT, "Stocks");

        mockMvc.perform(authed(jsonBody(post(API + "/budgets"), """
                        {"categoryId":%d,"limitAmount":100.00}
                        """.formatted(stocks)), user))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAndDeleteAGoal() throws Exception {
        TestUser user = registerUser();
        long food = seededCategoryId(user, CategoryType.EXPENSE, "Food");

        long goalId = readTree(mockMvc.perform(authed(jsonBody(post(API + "/budgets"), """
                        {"categoryId":%d,"limitAmount":18000.00}
                        """.formatted(food)), user))
                .andExpect(status().isCreated())
                .andReturn()).get("id").asLong();

        mockMvc.perform(authed(jsonBody(put(API + "/budgets/" + goalId), """
                        {"categoryId":%d,"limitAmount":20000.00}
                        """.formatted(food)), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limitAmount").value(20000.00));

        mockMvc.perform(authed(get(API + "/budgets/goals"), user))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(authed(delete(API + "/budgets/" + goalId), user))
                .andExpect(status().isNoContent());

        mockMvc.perform(authed(get(API + "/budgets").param("month", "2026-04"), user))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("a user cannot see or mutate another user's budget goals")
    void ownershipIsEnforced() throws Exception {
        TestUser alice = registerUser();
        TestUser bob = registerUser();

        long aliceFood = seededCategoryId(alice, CategoryType.EXPENSE, "Food");
        long bobFood = seededCategoryId(bob, CategoryType.EXPENSE, "Food");

        long aliceGoalId = readTree(mockMvc.perform(authed(jsonBody(post(API + "/budgets"), """
                        {"categoryId":%d,"limitAmount":18000.00}
                        """.formatted(aliceFood)), alice))
                .andExpect(status().isCreated())
                .andReturn()).get("id").asLong();

        mockMvc.perform(authed(get(API + "/budgets").param("month", "2026-04"), bob))
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(authed(jsonBody(put(API + "/budgets/" + aliceGoalId), """
                        {"categoryId":%d,"limitAmount":1.00}
                        """.formatted(bobFood)), bob))
                .andExpect(status().isForbidden());

        mockMvc.perform(authed(delete(API + "/budgets/" + aliceGoalId), bob))
                .andExpect(status().isForbidden());
    }

    private void createExpense(TestUser user, String description, String amount, long categoryId) throws Exception {
        createExpenseOn(user, description, amount, categoryId, "2026-04-10");
    }

    private void createExpenseOn(TestUser user, String description, String amount, long categoryId, String date)
            throws Exception {
        mockMvc.perform(authed(jsonBody(post(API + "/expenses"), """
                {"description":"%s","amount":%s,"categoryId":%d,"transactionDate":"%s"}
                """.formatted(description, amount, categoryId, date)), user))
                .andExpect(status().isCreated());
    }
}
