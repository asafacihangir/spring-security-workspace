package org.phoenix.rememberme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Faz 6 (UC-010 main scenario, Test Adimi 2 &amp; 4): the token-mode
 * counterpart of {@link PersistentRememberMeStrategyTests}. Runs against
 * the app's shipped default ({@code app.remember-me.strategy=token} in
 * {@code application.properties} - no {@code @TestPropertySource} override
 * needed, unlike the persistent-mode class), proving a remember-me login in
 * token mode leaves {@code persistent_logins} untouched, and that a
 * persistent-strategy-shaped cookie is rejected cleanly in token mode (A2),
 * mirroring the same check the other direction.
 *
 * <p>Runs against the real MySQL instance (task infra:up), same as the rest
 * of this suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RememberMeStrategyDoesNotWriteToPersistentLoginsTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long countPersistentLoginsForDemoUser() {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from persistent_logins where username = ?",
                Long.class, DemoUserSeeder.DEMO_USERNAME);
        return count == null ? 0 : count;
    }

    @Test
    void tokenModeLoginWritesNoRowToPersistentLogins() throws Exception {
        long before = countPersistentLoginsForDemoUser();

        MvcResult loginResult = mockMvc.perform(post("/api/login")
                        .param("username", DemoUserSeeder.DEMO_USERNAME)
                        .param("password", DemoUserSeeder.DEMO_PASSWORD)
                        .param("remember-me", "true"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie rememberMe = loginResult.getResponse().getCookie("remember-me");
        assertThat(rememberMe).isNotNull();

        long after = countPersistentLoginsForDemoUser();
        assertThat(after).isEqualTo(before);
    }

    @Test
    void a2PersistentStrategyShapedCookieIsRejectedCleanlyInTokenMode() throws Exception {
        // Shaped like a PersistentTokenBasedRememberMeServices cookie
        // (series:token, 2 parts) - TokenBasedRememberMeServices only
        // accepts 3 or 4 parts.
        String fakePersistentModeCookieValue = encode("some-series-value:some-token-value");
        Cookie crossStrategyCookie = new Cookie("remember-me", fakePersistentModeCookieValue);

        // A2: rejected cleanly (no auto-login, no 500) - falls through to the
        // normal "not authenticated" 401 a protected endpoint gives an
        // anonymous caller.
        mockMvc.perform(get("/api/me").cookie(crossStrategyCookie))
                .andExpect(status().isUnauthorized());
    }

    private static String encode(String plain) {
        return Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }

}
