package com.bharath.financetracker.support;

import com.bharath.financetracker.category.domain.CategoryType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the real application against a throwaway MySQL 8 container.
 *
 * <p>Flyway builds the schema and Hibernate then runs with {@code ddl-auto: validate}, so every
 * integration test doubles as a check that the migration and the entity mappings agree.
 *
 * <p>The container is a singleton started once per JVM. Tests do not share data: each one
 * registers its own users, so no cleanup between tests is needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    protected static final String API = "/api/v1";

    private static final AtomicLong EMAIL_SEQUENCE = new AtomicLong();

    @SuppressWarnings("resource")
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("finance_tracker")
            .withUsername("finance")
            .withPassword("finance");

    static {
        // Docker Engine 29 refuses API versions below 1.44, which is newer than the default
        // docker-java negotiates; without this every strategy fails with an opaque HTTP 400.
        // Override with -Dapi.version=... or DOCKER_API_VERSION for an older engine.
        if (System.getProperty("api.version") == null && System.getenv("DOCKER_API_VERSION") == null) {
            System.setProperty("api.version", "1.44");
        }
        MYSQL.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("app.jwt.secret", () -> "integration-test-signing-key-at-least-32-bytes-long");
    }

    @BeforeAll
    static void containerIsRunning() {
        if (!MYSQL.isRunning()) {
            throw new IllegalStateException("MySQL Testcontainer failed to start");
        }
    }

    // --- fixtures -------------------------------------------------------------------------

    /**
     * Registers a brand-new user and returns their id and access token.
     */
    protected TestUser registerUser() throws Exception {
        String email = "user%d@test.local".formatted(EMAIL_SEQUENCE.incrementAndGet());

        MvcResult registered = mockMvc.perform(post(API + "/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test User","email":"%s","password":"s3cure-passw0rd"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode tokens = readTree(registered);
        String accessToken = tokens.get("accessToken").asText();
        String refreshToken = tokens.get("refreshToken").asText();

        MvcResult me = mockMvc.perform(get(API + "/users/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andReturn();

        return new TestUser(readTree(me).get("id").asLong(), email, accessToken, refreshToken);
    }

    /**
     * Looks up one of the categories seeded at registration.
     */
    protected long seededCategoryId(TestUser user, CategoryType type, String name) throws Exception {
        MvcResult result = mockMvc.perform(authed(get(API + "/categories").param("type", type.name()), user))
                .andExpect(status().isOk())
                .andReturn();

        for (JsonNode category : readTree(result)) {
            if (name.equals(category.get("name").asText())) {
                return category.get("id").asLong();
            }
        }
        throw new AssertionError("No seeded %s category named '%s'".formatted(type, name));
    }

    // --- helpers --------------------------------------------------------------------------

    protected MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder builder, TestUser user) {
        return builder.header(HttpHeaders.AUTHORIZATION, bearer(user.accessToken()));
    }

    protected MockHttpServletRequestBuilder jsonBody(MockHttpServletRequestBuilder builder, String json) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(json);
    }

    protected JsonNode readTree(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    public record TestUser(long id, String email, String accessToken, String refreshToken) {
    }
}
