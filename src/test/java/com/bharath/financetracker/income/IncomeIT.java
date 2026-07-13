package com.bharath.financetracker.income;

import com.bharath.financetracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IncomeIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("create, list by month, update, delete")
    void crudHappyPath() throws Exception {
        TestUser user = registerUser();

        long id = readTree(mockMvc.perform(authed(jsonBody(post(API + "/income"), """
                        {"month":"2026-04","amount":191000.00,"source":"Salary"}
                        """), user))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.month").value("2026-04"))
                .andExpect(jsonPath("$.amount").value(191000.00))
                .andExpect(jsonPath("$.source").value("Salary"))
                .andReturn()).get("id").asLong();

        mockMvc.perform(authed(get(API + "/income").param("month", "2026-04"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // A different month sees nothing.
        mockMvc.perform(authed(get(API + "/income").param("month", "2026-05"), user))
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(authed(jsonBody(put(API + "/income/" + id), """
                        {"month":"2026-04","amount":200000.00,"source":"Salary + bonus"}
                        """), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(200000.00))
                .andExpect(jsonPath("$.source").value("Salary + bonus"));

        mockMvc.perform(authed(delete(API + "/income/" + id), user))
                .andExpect(status().isNoContent());

        mockMvc.perform(authed(get(API + "/income"), user))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("multiple income rows per month are allowed and all listed")
    void multipleRowsPerMonth() throws Exception {
        TestUser user = registerUser();

        mockMvc.perform(authed(jsonBody(post(API + "/income"), """
                {"month":"2026-04","amount":150000.00}
                """), user)).andExpect(status().isCreated())
                // `source` defaults to Salary when omitted.
                .andExpect(jsonPath("$.source").value("Salary"));

        mockMvc.perform(authed(jsonBody(post(API + "/income"), """
                {"month":"2026-04","amount":41000.00,"source":"Freelance"}
                """), user)).andExpect(status().isCreated());

        mockMvc.perform(authed(get(API + "/income").param("month", "2026-04"), user))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("a non-positive amount and a malformed month are both rejected")
    void validationRejectsBadInput() throws Exception {
        TestUser user = registerUser();

        mockMvc.perform(authed(jsonBody(post(API + "/income"), """
                        {"month":"2026-04","amount":0.00}
                        """), user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"));

        mockMvc.perform(authed(get(API + "/income").param("month", "April"), user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("expected YYYY-MM")));
    }

    @Test
    @DisplayName("a user cannot see or mutate another user's income")
    void ownershipIsEnforced() throws Exception {
        TestUser alice = registerUser();
        TestUser bob = registerUser();

        long aliceIncomeId = readTree(mockMvc.perform(authed(jsonBody(post(API + "/income"), """
                        {"month":"2026-04","amount":191000.00}
                        """), alice))
                .andExpect(status().isCreated())
                .andReturn()).get("id").asLong();

        mockMvc.perform(authed(get(API + "/income").param("month", "2026-04"), bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(authed(jsonBody(put(API + "/income/" + aliceIncomeId), """
                        {"month":"2026-04","amount":1.00}
                        """), bob))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(authed(delete(API + "/income/" + aliceIncomeId), bob))
                .andExpect(status().isForbidden());

        // Alice's row survived untouched.
        mockMvc.perform(authed(get(API + "/income").param("month", "2026-04"), alice))
                .andExpect(jsonPath("$[0].amount").value(191000.00));
    }
}
