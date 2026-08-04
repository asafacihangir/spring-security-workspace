package org.phoenix.rememberme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Faz 6 (UC-010 main scenario, Test Adimi 1 &amp; 4): with
 * {@code app.remember-me.strategy=persistent}, proves the app actually
 * builds {@code PersistentTokenBasedRememberMeServices} - not just that the
 * property is readable - by checking its one observable side effect: a
 * remember-me login writes a row to {@code persistent_logins}. Also covers
 * A2 (a token-strategy-shaped cookie presented while running in persistent
 * mode is rejected cleanly, never a 500).
 *
 * <p>Runs against the real MySQL instance (task infra:up), same as the rest
 * of this suite. {@code @TestPropertySource} gives this class its own
 * {@code ApplicationContext} (a different property set than the
 * {@code app.remember-me.strategy=token} default every other test class
 * uses), so the two strategies are never conflated within one running
 * context - see {@link RememberMeStrategyDoesNotWriteToPersistentLoginsTests}
 * for the token-mode counterpart.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.remember-me.strategy=persistent")
class PersistentRememberMeStrategyTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteRowsCreatedDuringTest() {
        // Keeps the shared dev database from accumulating rows across
        // repeated runs, same convention as NoteControllerTests.
        jdbcTemplate.update("delete from persistent_logins where username = ?", DemoUserSeeder.DEMO_USERNAME);
    }

    private long countPersistentLoginsForDemoUser() {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from persistent_logins where username = ?",
                Long.class, DemoUserSeeder.DEMO_USERNAME);
        return count == null ? 0 : count;
    }

    @Test
    void persistentModeLoginWritesARowToPersistentLogins() throws Exception {
        long before = countPersistentLoginsForDemoUser();

        MvcResult loginResult = mockMvc.perform(post("/api/login")
                        .param("username", DemoUserSeeder.DEMO_USERNAME)
                        .param("password", DemoUserSeeder.DEMO_PASSWORD)
                        .param("keep-me", "true"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie rememberMe = loginResult.getResponse().getCookie("notes-rm");
        assertThat(rememberMe).isNotNull();

        // Doubles as behavioral confirmation that PersistentTokenBasedRememberMeServices
        // (not TokenBasedRememberMeServices) is what actually issued this cookie:
        // its value decodes to exactly "series:token" - a plain HMAC-signed
        // TokenBasedRememberMeServices cookie always has 3-4 colon-separated
        // parts (username/expiry/[algorithm]/signature), never 2.
        String decoded = decode(rememberMe.getValue());
        assertThat(decoded.split(":")).hasSize(2);

        long after = countPersistentLoginsForDemoUser();
        assertThat(after).isEqualTo(before + 1);
    }

    @Test
    void a2TokenStrategyShapedCookieIsRejectedCleanlyInPersistentMode() throws Exception {
        // Shaped like a TokenBasedRememberMeServices cookie
        // (username:expiryTime:signature, 3 parts) - not a valid
        // "series:token" pair PersistentTokenBasedRememberMeServices expects.
        String fakeTokenModeCookieValue = encode(
                DemoUserSeeder.DEMO_USERNAME + ":9999999999999:deadbeefdeadbeefdeadbeefdeadbeef");
        Cookie crossStrategyCookie = new Cookie("notes-rm", fakeTokenModeCookieValue);

        // A2: rejected cleanly (no auto-login, no 500) - falls through to the
        // normal "not authenticated" 401 a protected endpoint gives an
        // anonymous caller.
        mockMvc.perform(get("/api/me").cookie(crossStrategyCookie))
                .andExpect(status().isUnauthorized());
    }

    private static String decode(String base64) {
        StringBuilder padded = new StringBuilder(base64);
        while (padded.length() % 4 != 0) {
            padded.append("=");
        }
        return new String(Base64.getDecoder().decode(padded.toString()), StandardCharsets.UTF_8);
    }

    private static String encode(String plain) {
        return Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }

}
