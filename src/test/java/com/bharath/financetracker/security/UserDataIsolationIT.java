package com.bharath.financetracker.security;

import com.bharath.financetracker.category.domain.CategoryType;
import com.bharath.financetracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The headline guarantee: two users' data is fully isolated.
 *
 * <p>Alice books ₹40,000 of April expenses; Bob books ₹500. Nothing Bob does can read, count, or
 * mutate Alice's rows.
 */
class UserDataIsolationIT extends AbstractIntegrationTest {

    private TestUser alice;
    private TestUser bob;
    private long aliceExpenseId;

    @BeforeEach
    void seedTwoUsers() throws Exception {
        alice = registerUser();
        bob = registerUser();

        long aliceFood = seededCategoryId(alice, CategoryType.EXPENSE, "Food");
        long aliceTravel = seededCategoryId(alice, CategoryType.EXPENSE, "Travel");
        long bobFood = seededCategoryId(bob, CategoryType.EXPENSE, "Food");

        aliceExpenseId = createExpense(alice, "Alice groceries", "16000.00", aliceFood);
        createExpense(alice, "Alice flight", "24000.00", aliceTravel);
        createExpense(bob, "Bob coffee", "500.00", bobFood);

        create("/income", alice, """
                {"month":"2026-04","amount":191000.00}""");
    }

    @Test
    @DisplayName("user A cannot read user B's expenses through the grouped month view")
    void groupedViewShowsOnlyOwnExpenses() throws Exception {
        // Bob's month totals only his own ₹500.
        mockMvc.perform(authed(get(API + "/expenses").param("month", "2026-04"), bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(500.00))
                .andExpect(jsonPath("$.categories.length()").value(1))
                .andExpect(jsonPath("$.categories[0].transactions[*].description").value(is(hasItem("Bob coffee"))))
                .andExpect(jsonPath("$.categories[0].transactions[*].description")
                        .value(not(hasItem("Alice groceries"))))
                .andExpect(jsonPath("$.categories[0].transactions[*].description")
                        .value(not(hasItem("Alice flight"))));

        // And Alice sees only hers.
        mockMvc.perform(authed(get(API + "/expenses").param("month", "2026-04"), alice))
                .andExpect(jsonPath("$.totalExpense").value(40000.00))
                .andExpect(jsonPath("$.categories.length()").value(2));
    }

    @Test
    @DisplayName("user A cannot read user B's expenses through the flat, paginated transaction list")
    void transactionListShowsOnlyOwnExpenses() throws Exception {
        mockMvc.perform(authed(get(API + "/expenses/transactions").param("size", "100"), bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[*].description").value(everyItem(is("Bob coffee"))));
    }

    @Test
    @DisplayName("user A cannot update or delete user B's expense: 403, and the row survives")
    void cannotMutateAnotherUsersExpense() throws Exception {
        long bobFood = seededCategoryId(bob, CategoryType.EXPENSE, "Food");

        mockMvc.perform(authed(jsonBody(put(API + "/expenses/" + aliceExpenseId), """
                        {"description":"Hijacked","amount":1.00,"categoryId":%d,"transactionDate":"2026-04-03"}
                        """.formatted(bobFood)), bob))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        mockMvc.perform(authed(delete(API + "/expenses/" + aliceExpenseId), bob))
                .andExpect(status().isForbidden());

        // Alice's expense is exactly as she left it.
        mockMvc.perform(authed(get(API + "/expenses").param("month", "2026-04"), alice))
                .andExpect(jsonPath("$.totalExpense").value(40000.00));
    }

    @Test
    @DisplayName("aggregates never leak: Bob's dashboard and report exclude Alice entirely")
    void aggregatesAreScopedToTheAuthenticatedUser() throws Exception {
        mockMvc.perform(authed(get(API + "/dashboard").param("month", "2026-04"), bob))
                .andExpect(jsonPath("$.metrics.salary").value(0.00))
                .andExpect(jsonPath("$.metrics.expenses").value(500.00))
                .andExpect(jsonPath("$.expenseBreakdown.length()").value(1));

        mockMvc.perform(authed(get(API + "/reports/summary").param("month", "2026-04"), bob))
                .andExpect(jsonPath("$.totals.salary").value(0.00))
                .andExpect(jsonPath("$.totals.expenses").value(500.00))
                .andExpect(jsonPath("$.topExpenseCategories.length()").value(1))
                .andExpect(jsonPath("$.topExpenseCategories[0].total").value(500.00));

        // Alice's own aggregates are unaffected by Bob's existence.
        mockMvc.perform(authed(get(API + "/dashboard").param("month", "2026-04"), alice))
                .andExpect(jsonPath("$.metrics.salary").value(191000.00))
                .andExpect(jsonPath("$.metrics.expenses").value(40000.00));
    }

    @Test
    @DisplayName("Bob's token cannot be swapped for Alice's identity")
    void tokensIdentifyTheirOwnUser() throws Exception {
        mockMvc.perform(authed(get(API + "/users/me"), bob))
                .andExpect(jsonPath("$.id").value(bob.id()))
                .andExpect(jsonPath("$.email").value(bob.email()));

        mockMvc.perform(authed(get(API + "/users/me"), alice))
                .andExpect(jsonPath("$.id").value(alice.id()))
                .andExpect(jsonPath("$.email").value(alice.email()));
    }

    private long createExpense(TestUser user, String description, String amount, long categoryId) throws Exception {
        return readTree(mockMvc.perform(authed(jsonBody(post(API + "/expenses"), """
                        {"description":"%s","amount":%s,"categoryId":%d,"transactionDate":"2026-04-03"}
                        """.formatted(description, amount, categoryId)), user))
                .andExpect(status().isCreated())
                .andReturn()).get("id").asLong();
    }

    private void create(String path, TestUser user, String body) throws Exception {
        mockMvc.perform(authed(jsonBody(post(API + path), body), user)).andExpect(status().isCreated());
    }
}
