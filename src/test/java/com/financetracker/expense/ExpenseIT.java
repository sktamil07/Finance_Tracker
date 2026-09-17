package com.financetracker.expense;

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

class ExpenseIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("create, update, delete")
    void crudHappyPath() throws Exception {
        TestUser user = registerUser();
        long food = seededCategoryId(user, CategoryType.EXPENSE, "Food");
        long travel = seededCategoryId(user, CategoryType.EXPENSE, "Travel");

        long id = readTree(mockMvc.perform(authed(jsonBody(post(API + "/expenses"), """
                        {"description":"Weekly groceries","amount":2450.75,"categoryId":%d,
                         "transactionDate":"2026-04-12","notes":"big shop"}
                        """.formatted(food)), user))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryName").value("Food"))
                .andExpect(jsonPath("$.amount").value(2450.75))
                .andExpect(jsonPath("$.notes").value("big shop"))
                .andReturn()).get("id").asLong();

        mockMvc.perform(authed(jsonBody(put(API + "/expenses/" + id), """
                        {"description":"Flight","amount":8900.00,"categoryId":%d,"transactionDate":"2026-04-09"}
                        """.formatted(travel)), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("Travel"))
                .andExpect(jsonPath("$.notes").doesNotExist());

        mockMvc.perform(authed(delete(API + "/expenses/" + id), user))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("a month's expenses come back grouped by category, biggest first, with each category's share")
    void groupedByCategory() throws Exception {
        TestUser user = registerUser();
        long food = seededCategoryId(user, CategoryType.EXPENSE, "Food");
        long travel = seededCategoryId(user, CategoryType.EXPENSE, "Travel");

        createExpense(user, "Groceries", "12000.00", food, "2026-04-03");
        createExpense(user, "Dining out", "4000.00", food, "2026-04-14");
        createExpense(user, "Flight", "24000.00", travel, "2026-04-09");
        // Outside the month: must not be counted.
        createExpense(user, "March groceries", "9999.00", food, "2026-03-31");

        mockMvc.perform(authed(get(API + "/expenses").param("month", "2026-04"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-04"))
                .andExpect(jsonPath("$.totalExpense").value(40000.00))
                .andExpect(jsonPath("$.categories.length()").value(2))
                // Travel (24000) outranks Food (16000).
                .andExpect(jsonPath("$.categories[0].categoryName").value("Travel"))
                .andExpect(jsonPath("$.categories[0].total").value(24000.00))
                .andExpect(jsonPath("$.categories[0].percentageOfMonth").value(60.00))
                .andExpect(jsonPath("$.categories[0].transactions.length()").value(1))
                .andExpect(jsonPath("$.categories[1].categoryName").value("Food"))
                .andExpect(jsonPath("$.categories[1].total").value(16000.00))
                .andExpect(jsonPath("$.categories[1].percentageOfMonth").value(40.00))
                .andExpect(jsonPath("$.categories[1].transactions.length()").value(2));
    }

    @Test
    @DisplayName("a month with no expenses returns zero totals rather than failing")
    void emptyMonth() throws Exception {
        TestUser user = registerUser();

        mockMvc.perform(authed(get(API + "/expenses").param("month", "2026-04"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(0.00))
                .andExpect(jsonPath("$.categories.length()").value(0));
    }

    @Test
    @DisplayName("the flat transaction list is paginated")
    void transactionsArePaginated() throws Exception {
        TestUser user = registerUser();
        long food = seededCategoryId(user, CategoryType.EXPENSE, "Food");

        for (int day = 1; day <= 5; day++) {
            createExpense(user, "Expense " + day, "100.00", food, "2026-04-0" + day);
        }

        mockMvc.perform(authed(get(API + "/expenses/transactions")
                        .param("month", "2026-04").param("page", "0").param("size", "2"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                // Default sort is transactionDate descending.
                .andExpect(jsonPath("$.content[0].description").value("Expense 5"));

        mockMvc.perform(authed(get(API + "/expenses/transactions")
                        .param("month", "2026-04").param("page", "2").param("size", "2"), user))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("an INVESTMENT category is rejected with 400 on an expense endpoint")
    void categoryTypeMustMatchTheEndpoint() throws Exception {
        TestUser user = registerUser();
        long stocks = seededCategoryId(user, CategoryType.INVESTMENT, "Stocks");

        mockMvc.perform(authed(jsonBody(post(API + "/expenses"), """
                        {"description":"Nope","amount":100.00,"categoryId":%d,"transactionDate":"2026-04-01"}
                        """.formatted(stocks)), user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("requires EXPENSE")));
    }

    @Test
    @DisplayName("a user cannot mutate another user's expense")
    void ownershipIsEnforced() throws Exception {
        TestUser alice = registerUser();
        TestUser bob = registerUser();

        long aliceFood = seededCategoryId(alice, CategoryType.EXPENSE, "Food");
        long bobFood = seededCategoryId(bob, CategoryType.EXPENSE, "Food");

        long aliceExpenseId = createExpense(alice, "Alice groceries", "12000.00", aliceFood, "2026-04-03");

        mockMvc.perform(authed(jsonBody(put(API + "/expenses/" + aliceExpenseId), """
                        {"description":"Hijacked","amount":1.00,"categoryId":%d,"transactionDate":"2026-04-03"}
                        """.formatted(bobFood)), bob))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(authed(delete(API + "/expenses/" + aliceExpenseId), bob))
                .andExpect(status().isForbidden());

        // Alice's row is untouched.
        mockMvc.perform(authed(get(API + "/expenses").param("month", "2026-04"), alice))
                .andExpect(jsonPath("$.totalExpense").value(12000.00));
    }

    private long createExpense(TestUser user, String description, String amount, long categoryId, String date)
            throws Exception {
        return readTree(mockMvc.perform(authed(jsonBody(post(API + "/expenses"), """
                        {"description":"%s","amount":%s,"categoryId":%d,"transactionDate":"%s"}
                        """.formatted(description, amount, categoryId, date)), user))
                .andExpect(status().isCreated())
                .andReturn()).get("id").asLong();
    }
}
