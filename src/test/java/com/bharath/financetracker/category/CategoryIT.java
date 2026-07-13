package com.bharath.financetracker.category;

import com.bharath.financetracker.category.domain.CategoryType;
import com.bharath.financetracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("registration seeds the SOP default categories")
    void defaultCategoriesAreSeeded() throws Exception {
        TestUser user = registerUser();

        mockMvc.perform(authed(get(API + "/categories").param("type", "EXPENSE"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "Food", "Travel", "Utilities", "Health", "Entertainment", "Shopping", "Other")))
                .andExpect(jsonPath("$[0].systemDefault").value(true));

        mockMvc.perform(authed(get(API + "/categories").param("type", "INVESTMENT"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(8))
                .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "Bonds", "Mutual Funds", "Stocks", "FD", "PPF", "NPS", "Gold", "Crypto")));

        mockMvc.perform(authed(get(API + "/categories"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(15));
    }

    @Test
    void createAndRenameACategory() throws Exception {
        TestUser user = registerUser();

        long id = readTree(mockMvc.perform(authed(jsonBody(post(API + "/categories"), """
                        {"name":"Groceries","type":"EXPENSE","icon":"shopping-cart"}
                        """), user))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.systemDefault").value(false))
                .andReturn()).get("id").asLong();

        mockMvc.perform(authed(jsonBody(put(API + "/categories/" + id), """
                        {"name":"Groceries & Food","icon":"basket"}
                        """), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Groceries & Food"))
                .andExpect(jsonPath("$.icon").value("basket"))
                // Type is immutable, so it is unchanged.
                .andExpect(jsonPath("$.type").value("EXPENSE"));
    }

    @Test
    @DisplayName("a duplicate name within the same type is a 409")
    void duplicateNameIsConflict() throws Exception {
        TestUser user = registerUser();

        mockMvc.perform(authed(jsonBody(post(API + "/categories"), """
                        {"name":"Food","type":"EXPENSE"}
                        """), user))
                .andExpect(status().isConflict());

        // The same name under a different type is fine.
        mockMvc.perform(authed(jsonBody(post(API + "/categories"), """
                        {"name":"Food","type":"INVESTMENT"}
                        """), user))
                .andExpect(status().isCreated());
    }

    @Test
    void unusedCategoryCanBeDeleted() throws Exception {
        TestUser user = registerUser();
        long id = seededCategoryId(user, CategoryType.EXPENSE, "Entertainment");

        mockMvc.perform(authed(delete(API + "/categories/" + id), user))
                .andExpect(status().isNoContent());

        mockMvc.perform(authed(get(API + "/categories").param("type", "EXPENSE"), user))
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    @DisplayName("deleting a category that is still in use is blocked with 409, never silently reassigned")
    void inUseCategoryCannotBeDeleted() throws Exception {
        TestUser user = registerUser();
        long foodId = seededCategoryId(user, CategoryType.EXPENSE, "Food");

        mockMvc.perform(authed(jsonBody(post(API + "/expenses"), """
                        {"description":"Groceries","amount":2450.75,"categoryId":%d,"transactionDate":"2026-04-12"}
                        """.formatted(foodId)), user))
                .andExpect(status().isCreated());

        mockMvc.perform(authed(delete(API + "/categories/" + foodId), user))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("still in use")));
    }

    @Test
    @DisplayName("a user cannot see, rename, or delete another user's category")
    void categoriesAreIsolatedPerUser() throws Exception {
        TestUser alice = registerUser();
        TestUser bob = registerUser();

        long aliceCategoryId = readTree(mockMvc.perform(authed(jsonBody(post(API + "/categories"), """
                        {"name":"Alice Only","type":"EXPENSE"}
                        """), alice))
                .andExpect(status().isCreated())
                .andReturn()).get("id").asLong();

        // Bob's list never contains Alice's category.
        mockMvc.perform(authed(get(API + "/categories"), bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Alice Only"))));

        mockMvc.perform(authed(jsonBody(put(API + "/categories/" + aliceCategoryId), """
                        {"name":"Hijacked"}
                        """), bob))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(authed(delete(API + "/categories/" + aliceCategoryId), bob))
                .andExpect(status().isForbidden());

        // Alice's category is untouched.
        mockMvc.perform(authed(get(API + "/categories"), alice))
                .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.hasItem("Alice Only")));
    }

    @Test
    @DisplayName("a category that does not exist at all is a 404, not a 403")
    void unknownCategoryIsNotFound() throws Exception {
        TestUser user = registerUser();

        mockMvc.perform(authed(delete(API + "/categories/99999999"), user))
                .andExpect(status().isNotFound());
    }
}
