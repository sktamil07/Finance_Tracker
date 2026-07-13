package com.bharath.financetracker.auth;

import com.bharath.financetracker.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("register issues a token pair and seeds the 15 default categories")
    void registerHappyPath() throws Exception {
        mockMvc.perform(jsonBody(post(API + "/auth/register"), """
                        {"name":"Bharath","email":"register-happy@test.local","password":"s3cure-passw0rd"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("registering the same email twice is a 409")
    void duplicateEmailIsConflict() throws Exception {
        String body = """
                {"name":"Bharath","email":"dupe@test.local","password":"s3cure-passw0rd"}
                """;
        mockMvc.perform(jsonBody(post(API + "/auth/register"), body)).andExpect(status().isCreated());

        mockMvc.perform(jsonBody(post(API + "/auth/register"), body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value(API + "/auth/register"))
                .andExpect(jsonPath("$.message").value("An account with that email already exists"));
    }

    @Test
    @DisplayName("a short password is rejected with per-field validation errors")
    void validationErrorsCarryFieldDetails() throws Exception {
        mockMvc.perform(jsonBody(post(API + "/auth/register"), """
                        {"name":"","email":"not-an-email","password":"short"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[*].field").value(
                        org.hamcrest.Matchers.containsInAnyOrder("email", "name", "password")));
    }

    @Test
    void loginReturnsATokenPair() throws Exception {
        TestUser user = registerUser();

        mockMvc.perform(jsonBody(post(API + "/auth/login"), """
                        {"email":"%s","password":"s3cure-passw0rd"}
                        """.formatted(user.email())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("a wrong password is 401 and never reveals whether the email exists")
    void wrongPasswordIsUnauthorized() throws Exception {
        TestUser user = registerUser();

        mockMvc.perform(jsonBody(post(API + "/auth/login"), """
                        {"email":"%s","password":"wrong-password"}
                        """.formatted(user.email())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        mockMvc.perform(jsonBody(post(API + "/auth/login"), """
                        {"email":"nobody@test.local","password":"wrong-password"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void refreshRotatesTheTokenPair() throws Exception {
        TestUser user = registerUser();

        mockMvc.perform(jsonBody(post(API + "/auth/refresh"), """
                        {"refreshToken":"%s"}
                        """.formatted(user.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("an access token cannot be replayed at /auth/refresh")
    void accessTokenIsRejectedAtRefresh() throws Exception {
        TestUser user = registerUser();

        mockMvc.perform(jsonBody(post(API + "/auth/refresh"), """
                        {"refreshToken":"%s"}
                        """.formatted(user.accessToken())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a refresh token cannot be used as a bearer token on a protected endpoint")
    void refreshTokenIsRejectedAsBearer() throws Exception {
        TestUser user = registerUser();

        mockMvc.perform(get(API + "/users/me")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, bearer(user.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("A refresh token cannot be used to access resources"));
    }

    @Test
    void protectedEndpointsRequireAToken() throws Exception {
        mockMvc.perform(get(API + "/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get(API + "/users/me").header(
                        org.springframework.http.HttpHeaders.AUTHORIZATION, bearer("garbage.token.value")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired access token"));
    }

    @Test
    void usersMeReturnsTheAuthenticatedProfile() throws Exception {
        TestUser user = registerUser();

        mockMvc.perform(authed(get(API + "/users/me"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.id()))
                .andExpect(jsonPath("$.email").value(user.email()))
                .andExpect(jsonPath("$.currencyCode").value("INR"))
                .andExpect(jsonPath("$.themePreference").value("SYSTEM"))
                // The password hash must never be serialised.
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
}
