package com.financetracker.settings;

import com.financetracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettingsIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("a new user starts on INR and the SYSTEM theme")
    void defaultsHappyPath() throws Exception {
        TestUser user = registerUser();

        mockMvc.perform(authed(get(API + "/settings"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.email").value(user.email()))
                .andExpect(jsonPath("$.currencyCode").value("INR"))
                .andExpect(jsonPath("$.themePreference").value("SYSTEM"));
    }

    @Test
    @DisplayName("updating settings persists and shows up on /users/me")
    void updateHappyPath() throws Exception {
        TestUser user = registerUser();

        mockMvc.perform(authed(jsonBody(put(API + "/settings"), """
                        {"displayName":"Bharath","currencyCode":"USD","themePreference":"DARK"}
                        """), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Bharath"))
                .andExpect(jsonPath("$.currencyCode").value("USD"))
                .andExpect(jsonPath("$.themePreference").value("DARK"));

        mockMvc.perform(authed(get(API + "/users/me"), user))
                .andExpect(jsonPath("$.name").value("Bharath"))
                .andExpect(jsonPath("$.currencyCode").value("USD"))
                .andExpect(jsonPath("$.themePreference").value("DARK"));

        // The dashboard reports the user's chosen currency.
        mockMvc.perform(authed(get(API + "/dashboard").param("month", "2026-04"), user))
                .andExpect(jsonPath("$.currencyCode").value("USD"));
    }

    @Test
    @DisplayName("a bad currency code or unknown theme is rejected")
    void validationRejectsBadInput() throws Exception {
        TestUser user = registerUser();

        mockMvc.perform(authed(jsonBody(put(API + "/settings"), """
                        {"displayName":"Bharath","currencyCode":"rupees","themePreference":"DARK"}
                        """), user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("currencyCode"));

        mockMvc.perform(authed(jsonBody(put(API + "/settings"), """
                        {"displayName":"Bharath","currencyCode":"INR","themePreference":"NEON"}
                        """), user))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("settings are per user: updating one never touches the other")
    void settingsAreIsolatedPerUser() throws Exception {
        TestUser alice = registerUser();
        TestUser bob = registerUser();

        mockMvc.perform(authed(jsonBody(put(API + "/settings"), """
                {"displayName":"Alice","currencyCode":"EUR","themePreference":"LIGHT"}
                """), alice)).andExpect(status().isOk());

        mockMvc.perform(authed(get(API + "/settings"), bob))
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.currencyCode").value("INR"))
                .andExpect(jsonPath("$.themePreference").value("SYSTEM"));
    }

    @Test
    void settingsRequireAuthentication() throws Exception {
        mockMvc.perform(get(API + "/settings")).andExpect(status().isUnauthorized());
    }
}
