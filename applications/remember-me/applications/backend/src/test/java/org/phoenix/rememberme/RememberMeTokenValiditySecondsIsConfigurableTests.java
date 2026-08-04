package org.phoenix.rememberme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Faz 4 (İş Adımı 1): proves {@code app.remember-me.token-validity-seconds}
 * is not just present in {@code application.properties} but actually wired
 * through to {@code TokenBasedRememberMeServices.tokenValiditySeconds(...)}
 * in {@link SecurityConfig} - overriding it here to a small, distinctive
 * value and checking the resulting cookie's encoded expiry reflects that
 * value (not Spring's 14-day default) is the difference between "the
 * property exists" and "the property does something".
 *
 * <p>Uses {@code @TestPropertySource} to run this one test class against a
 * separate Spring context with the override, rather than touching the
 * shared {@code application.properties} default (1209600s / 14 days) that
 * every other test class in this suite relies on.
 *
 * <p>{@code cleanup-interval-ms=999999999} mirrors
 * {@link ExpiredPersistentLoginCleanupJobTests}'s same override, for the
 * same reason: {@code @Scheduled(fixedRateString=...)} runs its first
 * execution immediately on context startup (no implicit initial delay), and
 * this class's own {@code token-validity-seconds=5} would otherwise make
 * that very first run treat anything older than 5 seconds as expired -
 * against the one shared MySQL instance every test class in this suite
 * points at. This class only exercises token-mode remember-me and never
 * asserts on {@code persistent_logins} rows itself, so nothing here was ever
 * observed to break from that race, but the hazard is real (a
 * persistent-mode test creating rows around the same wall-clock moment this
 * context boots) and not worth relying on timing to avoid.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.remember-me.token-validity-seconds=5",
        "app.remember-me.cleanup-interval-ms=999999999"
})
class RememberMeTokenValiditySecondsIsConfigurableTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void configuredTokenValiditySecondsControlsTheIssuedCookiesExpiry() throws Exception {
        long beforeLogin = System.currentTimeMillis();

        MvcResult loginResult = mockMvc.perform(post("/api/login").with(csrf())
                        .param("username", DemoUserSeeder.DEMO_USERNAME)
                        .param("password", DemoUserSeeder.DEMO_PASSWORD)
                        .param("keep-me", "true"))
                .andExpect(status().isOk())
                .andReturn();

        long afterLogin = System.currentTimeMillis();
        Cookie rememberMe = loginResult.getResponse().getCookie("notes-rm");
        assertThat(rememberMe).isNotNull();

        long expiryTime = decodeExpiryTimeMillis(rememberMe.getValue());

        // With the property overridden to 5s, expiry must land ~5s after
        // login - nowhere near Spring's 1,209,600s (14-day) default, which
        // is what would come out if the property were silently ignored.
        assertThat(expiryTime).isBetween(
                beforeLogin + Duration.ofSeconds(5).toMillis(),
                afterLogin + Duration.ofSeconds(5).toMillis() + Duration.ofSeconds(1).toMillis());
    }

    /**
     * Inverse of {@code AbstractRememberMeServices.decodeCookie} /
     * {@code TokenBasedRememberMeServices}'s cookie layout: base64, then
     * {@code ":"}-delimited {@code username:expiryTime:algorithmName:signature}.
     */
    private static long decodeExpiryTimeMillis(String cookieValue) {
        StringBuilder padded = new StringBuilder(cookieValue);
        while (padded.length() % 4 != 0) {
            padded.append("=");
        }
        String plain = new String(Base64.getDecoder().decode(padded.toString()), StandardCharsets.UTF_8);
        String[] tokens = plain.split(":");
        return Long.parseLong(tokens[1]);
    }

}
