package org.phoenix.rememberme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
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
 * Faz 10 (UC-016, UC-017, FR-016/FR-017, BR-023/BR-024) with
 * {@code app.remember-me.strategy=persistent} and
 * {@code app.remember-me.ip-binding-enabled=true}: proves the actual
 * IP-binding behavior end to end through the real
 * {@code RememberMeAuthenticationFilter} auto-login path, the same
 * MockMvc-driven-HTTP-request style {@link TokenRotationTests}/
 * {@link StolenCookieDetectionTests} already use for the rest of the
 * persistent-mode suite.
 *
 * <p><b>How "a different IP" is simulated:</b> {@code MockHttpServletRequestBuilder.remoteAddress(String)}
 * sets {@code HttpServletRequest.getRemoteAddr()} directly for the fabricated
 * request - the same value {@link IpBoundPersistentTokenBasedRememberMeServices}
 * reads to decide both what to record and what to check. This is the one
 * genuinely reliable way to exercise a same-IP-vs-different-IP comparison
 * in an automated test without a second real network path - see the
 * README's "IP-Bound Remember-Me" section for why this is also, honestly,
 * the most practical way to verify this feature at all in a single-machine
 * dev/CI environment (manually reproducing two *actually* different client
 * IPs against one locally-run backend is not reliably possible the way
 * {@code localhost} vs {@code JSESSIONID} deletion was for earlier phases).
 *
 * <p>See {@link IpBindingDisabledTests} for the A2/BR-023 "binding off"
 * counterpart and the token-strategy regression check.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.remember-me.strategy=persistent",
        "app.remember-me.ip-binding-enabled=true"
})
class IpBoundRememberMeTests {

    private static final String ORIGIN_IP = "203.0.113.10";
    private static final String OTHER_IP = "203.0.113.99";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteRowsCreatedDuringTest() {
        jdbcTemplate.update("delete from persistent_logins where username = ?", DemoUserSeeder.DEMO_USERNAME);
    }

    private Cookie loginWithRememberMeFrom(String remoteAddress) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login").with(csrf())
                        .param("username", DemoUserSeeder.DEMO_USERNAME)
                        .param("password", DemoUserSeeder.DEMO_PASSWORD)
                        .param("keep-me", "true")
                        .remoteAddress(remoteAddress))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("notes-rm");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    /** UC-016 main scenario step 2 / FR-016: the record is bound to the login's client IP. */
    @Test
    void loginRecordsTheClientIpAgainstTheNewSeries() throws Exception {
        loginWithRememberMeFrom(ORIGIN_IP);

        String boundIp = jdbcTemplate.queryForObject(
                "select bound_ip from persistent_logins where username = ?", String.class, DemoUserSeeder.DEMO_USERNAME);
        assertThat(boundIp).isEqualTo(ORIGIN_IP);
    }

    /** UC-016 A1: auto-login from the exact IP the record was bound to succeeds normally. */
    @Test
    void a1_autoLoginFromTheSameIpSucceeds() throws Exception {
        Cookie cookie = loginWithRememberMeFrom(ORIGIN_IP);

        mockMvc.perform(get("/api/me").cookie(cookie).remoteAddress(ORIGIN_IP))
                .andExpect(status().isOk());
    }

    /** UC-016 main scenario steps 3-6 / BR-023: auto-login from a different IP is rejected cleanly. */
    @Test
    void mainScenario_autoLoginFromADifferentIpIsRejectedCleanlyNot500() throws Exception {
        Cookie cookie = loginWithRememberMeFrom(ORIGIN_IP);

        // BR-023: rejected - and "rejected" must mean the normal
        // "not authenticated" 401 every other invalid remember-me attempt
        // gets in this app, never an unhandled exception/500. See
        // IpBoundPersistentTokenBasedRememberMeServices.processAutoLoginCookie's
        // javadoc for exactly which exception type makes that true.
        mockMvc.perform(get("/api/me").cookie(cookie).remoteAddress(OTHER_IP))
                .andExpect(status().isUnauthorized());
    }

    /**
     * BR-023/UC-016 postcondition, and the "Ordering" design decision in
     * {@link IpBoundPersistentTokenBasedRememberMeServices#processAutoLoginCookie}'s
     * javadoc: a rejected different-IP attempt must not rotate the token or
     * otherwise touch the stored row - the legitimate owner's own copy of
     * the cookie, from the correct IP, must still work afterward exactly as
     * if the rejected attempt never happened.
     */
    @Test
    void rejectedDifferentIpAttemptDoesNotMutateTheStoredRowOrBreakTheLegitimateCookie() throws Exception {
        Cookie cookie = loginWithRememberMeFrom(ORIGIN_IP);
        String[] beforeSeriesAndToken = PersistentCookieCodec.seriesAndToken(cookie);

        mockMvc.perform(get("/api/me").cookie(cookie).remoteAddress(OTHER_IP))
                .andExpect(status().isUnauthorized());

        String tokenAfterRejectedAttempt = jdbcTemplate.queryForObject(
                "select token from persistent_logins where series = ?", String.class, beforeSeriesAndToken[0]);
        assertThat(tokenAfterRejectedAttempt)
                .as("a rejected cross-IP attempt must not rotate the token")
                .isEqualTo(beforeSeriesAndToken[1]);

        // The legitimate owner's original cookie, from the correct IP, is
        // completely unaffected by the rejected attempt above.
        mockMvc.perform(get("/api/me").cookie(cookie).remoteAddress(ORIGIN_IP))
                .andExpect(status().isOk());
    }

    /** UC-017 main scenario step 3 / BR-024: the Inspector shows the bound IP for the record. */
    @Test
    void tokenInspectorShowsTheBoundIpForTheRecord() throws Exception {
        loginWithRememberMeFrom(ORIGIN_IP);

        mockMvc.perform(get("/api/token-inspector"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].boundIp").value(ORIGIN_IP));
    }

}
