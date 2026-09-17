package com.financetracker.investment;

import com.financetracker.category.domain.CategoryType;
import com.financetracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InvestmentIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("create, list by month, update, delete")
    void crudHappyPath() throws Exception {
        TestUser user = registerUser();
        long bonds = seededCategoryId(user, CategoryType.INVESTMENT, "Bonds");
        long stocks = seededCategoryId(user, CategoryType.INVESTMENT, "Stocks");

        long id = readTree(mockMvc.perform(authed(jsonBody(post(API + "/investments"), """
                        {"name":"RBI Floating Rate Bond","amount":50000.00,"categoryId":%d,"month":"2026-04",
                         "roiNotes":"7.35%% p.a.","maturityDate":"2026-05-18"}
                        """.formatted(bonds)), user))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryName").value("Bonds"))
                .andExpect(jsonPath("$.month").value("2026-04"))
                .andExpect(jsonPath("$.roiNotes").value("7.35% p.a."))
                .andExpect(jsonPath("$.maturityDate").value("2026-05-18"))
                .andExpect(jsonPath("$.recurring").value(false))
                .andReturn()).get("id").asLong();

        mockMvc.perform(authed(get(API + "/investments").param("month", "2026-04"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(authed(get(API + "/investments").param("month", "2026-03"), user))
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(authed(jsonBody(put(API + "/investments/" + id), """
                        {"name":"HDFC Shares","amount":15000.00,"categoryId":%d,"month":"2026-04"}
                        """.formatted(stocks)), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryName").value("Stocks"))
                .andExpect(jsonPath("$.amount").value(15000.00))
                .andExpect(jsonPath("$.maturityDate").doesNotExist());

        mockMvc.perform(authed(delete(API + "/investments/" + id), user))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("an EXPENSE category is rejected with 400 on an investment endpoint")
    void categoryTypeMustMatchTheEndpoint() throws Exception {
        TestUser user = registerUser();
        long food = seededCategoryId(user, CategoryType.EXPENSE, "Food");

        mockMvc.perform(authed(jsonBody(post(API + "/investments"), """
                        {"name":"Nope","amount":100.00,"categoryId":%d,"month":"2026-04"}
                        """.formatted(food)), user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("requires INVESTMENT")));
    }

    @Test
    @DisplayName("a recurring investment must carry a SIP day")
    void recurringRequiresDayOfMonth() throws Exception {
        TestUser user = registerUser();
        long mutualFunds = seededCategoryId(user, CategoryType.INVESTMENT, "Mutual Funds");

        mockMvc.perform(authed(jsonBody(post(API + "/investments"), """
                        {"name":"SIP","amount":25000.00,"categoryId":%d,"month":"2026-04","recurring":true}
                        """.formatted(mutualFunds)), user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].message").value(
                        org.hamcrest.Matchers.containsString("recurringDayOfMonth is required")));
    }

    @Test
    @DisplayName("upcoming surfaces maturities and SIPs falling between today and the end of next month")
    void upcomingReturnsMaturitiesAndSips() throws Exception {
        TestUser user = registerUser();
        long bonds = seededCategoryId(user, CategoryType.INVESTMENT, "Bonds");

        LocalDate soon = LocalDate.now().plusDays(3);

        mockMvc.perform(authed(jsonBody(post(API + "/investments"), """
                {"name":"Maturing Bond","amount":50000.00,"categoryId":%d,"month":"2026-04","maturityDate":"%s"}
                """.formatted(bonds, soon)), user)).andExpect(status().isCreated());

        // A maturity far outside the window must not appear.
        mockMvc.perform(authed(jsonBody(post(API + "/investments"), """
                {"name":"Distant Bond","amount":10000.00,"categoryId":%d,"month":"2026-04","maturityDate":"%s"}
                """.formatted(bonds, LocalDate.now().plusYears(2))), user)).andExpect(status().isCreated());

        mockMvc.perform(authed(get(API + "/investments/upcoming"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.hasItem("Maturing Bond")))
                .andExpect(jsonPath("$[*].name").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Distant Bond"))))
                .andExpect(jsonPath("$[0].kind").value("MATURITY"))
                .andExpect(jsonPath("$[0].dueDate").value(soon.toString()));
    }

    @Test
    @DisplayName("a user cannot see or mutate another user's investments")
    void ownershipIsEnforced() throws Exception {
        TestUser alice = registerUser();
        TestUser bob = registerUser();

        long aliceBonds = seededCategoryId(alice, CategoryType.INVESTMENT, "Bonds");
        long bobBonds = seededCategoryId(bob, CategoryType.INVESTMENT, "Bonds");

        long aliceInvestmentId = readTree(mockMvc.perform(authed(jsonBody(post(API + "/investments"), """
                        {"name":"Alice Bond","amount":50000.00,"categoryId":%d,"month":"2026-04"}
                        """.formatted(aliceBonds)), alice))
                .andExpect(status().isCreated())
                .andReturn()).get("id").asLong();

        mockMvc.perform(authed(get(API + "/investments").param("month", "2026-04"), bob))
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(authed(jsonBody(put(API + "/investments/" + aliceInvestmentId), """
                        {"name":"Hijacked","amount":1.00,"categoryId":%d,"month":"2026-04"}
                        """.formatted(bobBonds)), bob))
                .andExpect(status().isForbidden());

        mockMvc.perform(authed(delete(API + "/investments/" + aliceInvestmentId), bob))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("borrowing another user's category id is a 403, not a silent cross-user write")
    void cannotAttachAnotherUsersCategory() throws Exception {
        TestUser alice = registerUser();
        TestUser bob = registerUser();

        long aliceBonds = seededCategoryId(alice, CategoryType.INVESTMENT, "Bonds");

        mockMvc.perform(authed(jsonBody(post(API + "/investments"), """
                        {"name":"Bob's","amount":100.00,"categoryId":%d,"month":"2026-04"}
                        """.formatted(aliceBonds)), bob))
                .andExpect(status().isForbidden());
    }
}
