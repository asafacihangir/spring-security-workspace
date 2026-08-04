package org.phoenix.rememberme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Faz 10 (UC-016 A2, UC-017 A1, BR-023, Checkpoint 10 regression check):
 * the counterpart to {@link IpBoundRememberMeTests} - proves the IP check
 * is only ever active when {@code app.remember-me.ip-binding-enabled=true},
 * and that leaving it at its default {@code false} reproduces Faz 3-9's
 * exact persistent-mode behavior with nothing new observable.
 */
class IpBindingDisabledTests {

    private static final String ORIGIN_IP = "203.0.113.10";
    private static final String OTHER_IP = "203.0.113.99";

    /**
     * UC-016 A2 / BR-023: {@code app.remember-me.strategy=persistent} with
     * IP-binding left at its default {@code false} - Faz 6/7's persistent
     * mode, unmodified. A record is still created (Faz 6 behavior), but no
     * IP is ever recorded on it, and auto-login never checks one - a
     * different-IP auto-login is accepted exactly like same-IP would be.
     */
    @SpringBootTest
    @AutoConfigureMockMvc
    @TestPropertySource(properties = "app.remember-me.strategy=persistent")
    @Nested
    class BindingOffUnderPersistentStrategy {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @AfterEach
        void deleteRowsCreatedDuringTest() {
            jdbcTemplate.update("delete from persistent_logins where username = ?", DemoUserSeeder.DEMO_USERNAME);
        }

        private Cookie loginWithRememberMeFrom(String remoteAddress) throws Exception {
            MvcResult result = mockMvc.perform(post("/api/login")
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

        @Test
        void a2_autoLoginFromADifferentIpIsAcceptedWhenBindingIsDisabled() throws Exception {
            Cookie cookie = loginWithRememberMeFrom(ORIGIN_IP);

            // BR-023: the check is only ever active when the property is on -
            // with it left at the default false, a different IP is accepted
            // exactly like Faz 6/7's persistent mode always has.
            mockMvc.perform(get("/api/me").cookie(cookie).remoteAddress(OTHER_IP))
                    .andExpect(status().isOk());
        }

        /** UC-017 A1: no IP was ever associated with the record, so the Inspector must say so explicitly. */
        @Test
        void tokenInspectorReportsNoBoundIpForTheRecord() throws Exception {
            loginWithRememberMeFrom(ORIGIN_IP);

            mockMvc.perform(get("/api/token-inspector"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.records[0].boundIp").doesNotExist());
        }
    }

    /**
     * Checkpoint 10 regression check: {@code app.remember-me.strategy=token}
     * with {@code app.remember-me.ip-binding-enabled=true} - proves the
     * property has zero effect under the token strategy.
     * {@link IpBoundPersistentTokenBasedRememberMeServices} is a persistent-mode-only
     * class ({@link SecurityConfig#filterChain} only ever constructs it
     * inside the PERSISTENT branch); this test's login/auto-login round
     * trip goes entirely through Faz 3/9's unmodified
     * {@code TokenBasedRememberMeServices} path regardless of this
     * property's value.
     */
    @SpringBootTest
    @AutoConfigureMockMvc
    @TestPropertySource(properties = {
            "app.remember-me.strategy=token",
            "app.remember-me.ip-binding-enabled=true"
    })
    @Nested
    class IpBindingEnabledHasNoEffectUnderTokenStrategy {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void loginAndCrossIpAutoLoginBothWorkExactlyAsInFaz3_NoIpCheckIsEverPerformed() throws Exception {
            MvcResult loginResult = mockMvc.perform(post("/api/login")
                            .param("username", DemoUserSeeder.DEMO_USERNAME)
                            .param("password", DemoUserSeeder.DEMO_PASSWORD)
                            .param("keep-me", "true")
                            .remoteAddress(ORIGIN_IP))
                    .andExpect(status().isOk())
                    .andReturn();
            Cookie cookie = loginResult.getResponse().getCookie("notes-rm");
            assertThat(cookie).isNotNull();

            // Token-based cookies carry no server-side record and no IP
            // concept at all (see IpBoundPersistentTokenBasedRememberMeServices's
            // "Scope decision" javadoc) - a "different IP" auto-login works
            // exactly like a same-IP one always has.
            mockMvc.perform(get("/api/me").cookie(cookie).remoteAddress(OTHER_IP))
                    .andExpect(status().isOk());
        }

        @Test
        void tokenInspectorStillReportsTokenStrategyWithNoRecords() throws Exception {
            mockMvc.perform(get("/api/token-inspector"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.strategy").value("TOKEN"))
                    .andExpect(jsonPath("$.records").isEmpty());
        }
    }

}
