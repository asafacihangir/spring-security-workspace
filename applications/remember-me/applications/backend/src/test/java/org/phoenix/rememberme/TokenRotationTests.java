package org.phoenix.rememberme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.Map;
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
 * Faz 7 (UC-011, Test Adimi 5): proves BR-015/016 end to end through the
 * real {@code PersistentTokenBasedRememberMeServices} auto-login path -
 * not by unit-testing {@code PersistentTokenRepository} in isolation, but
 * by driving an actual HTTP request through the same
 * {@code RememberMeAuthenticationFilter} a browser would hit.
 *
 * <p>The "session loss" step of UC-011's main scenario (step 2-3: end the
 * session, then request a protected page) is reproduced simply by never
 * attaching a session/{@code JSESSIONID} to the second request - only the
 * remember-me cookie - which is exactly what happens after a
 * {@code JSESSIONID} deletion or backend restart (see README's "Session
 * Kaybini Simule Etme" section for the equivalent manual steps).
 *
 * <p>This is not building rotation (Faz 7's brief is explicit that Spring
 * Security's own {@code processAutoLoginCookie} already does this - see
 * that method's source, quoted in task-7-report.md) - it is verifying it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.remember-me.strategy=persistent")
class TokenRotationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteRowsCreatedDuringTest() {
        jdbcTemplate.update("delete from persistent_logins where username = ?", DemoUserSeeder.DEMO_USERNAME);
    }

    private Cookie loginWithRememberMe() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login")
                        .param("username", DemoUserSeeder.DEMO_USERNAME)
                        .param("password", DemoUserSeeder.DEMO_PASSWORD)
                        .param("keep-me", "true"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("notes-rm");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    @Test
    void br015br016_autoLoginRotatesTokenButKeepsSeriesStable() throws Exception {
        Cookie originalCookie = loginWithRememberMe();
        String[] original = PersistentCookieCodec.seriesAndToken(originalCookie);
        String originalSeries = original[0];
        String originalToken = original[1];

        // UC-011 main scenario steps 2-4: no session attached, only the
        // remember-me cookie - this is what forces auto-login rather than a
        // session-backed request.
        MvcResult autoLoginResult = mockMvc.perform(get("/api/me").cookie(originalCookie))
                .andExpect(status().isOk())
                .andReturn();

        Cookie rotatedCookie = autoLoginResult.getResponse().getCookie("notes-rm");
        assertThat(rotatedCookie).as("auto-login must reissue the remember-me cookie with a new token")
                .isNotNull();
        String[] rotated = PersistentCookieCodec.seriesAndToken(rotatedCookie);

        // BR-016: series is the record's fixed identity.
        assertThat(rotated[0]).isEqualTo(originalSeries);
        // BR-015: token itself is renewed on every successful auto-login.
        assertThat(rotated[1]).isNotEqualTo(originalToken);

        // And the database row itself - not just the cookie the client sees -
        // reflects exactly that: same series, new token, still exactly one row.
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "select series, token from persistent_logins where username = ?", DemoUserSeeder.DEMO_USERNAME);
        assertThat(row.get("series")).isEqualTo(originalSeries);
        assertThat(row.get("token")).isEqualTo(rotated[1]);
        Long rowCount = jdbcTemplate.queryForObject(
                "select count(*) from persistent_logins where username = ?", Long.class, DemoUserSeeder.DEMO_USERNAME);
        assertThat(rowCount).isEqualTo(1L);
    }

}
