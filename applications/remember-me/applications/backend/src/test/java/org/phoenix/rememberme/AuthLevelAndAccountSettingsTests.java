package org.phoenix.rememberme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Faz 5 (UC-007, UC-008, UC-009): automated coverage for the parts of this
 * phase that don't require a browser -
 * <ul>
 * <li>{@code /api/auth-status} reports the right level for all three
 * states (BR-009);
 * <li>{@code /api/account} is genuinely blocked in the backend for a
 * Remembered caller, not just hidden in the UI (BR-010/BR-011 - Test
 * Adımı 5's "403/redirect" half - in practice Spring Security's
 * {@code ExceptionTranslationFilter} routes a failed
 * {@code isFullyAuthenticated()} check for a
 * {@code RememberMeAuthenticationToken} to the {@code AuthenticationEntryPoint}
 * rather than the {@code AccessDeniedHandler}, same as it does for a fully
 * anonymous caller, so this app's configured entry point replies 401, not
 * 403 - the brief accepts either);
 * <li>{@code /api/reauthenticate} upgrades a Remembered session to Fully
 * Authenticated on the correct password, and leaves it exactly as
 * Remembered on the wrong one (UC-009 main scenario + A1), always checking
 * the CURRENTLY authenticated principal's own password (BR-012).
 * </ul>
 *
 * <p>Runs against the real MySQL instance (task infra:up), same as the
 * rest of this suite, and logs in as the demo account seeded by
 * {@link DemoUserSeeder}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthLevelAndAccountSettingsTests {

    @Autowired
    private MockMvc mockMvc;

    private MvcResult login(boolean rememberMe) throws Exception {
        var request = post("/api/login")
                .param("username", DemoUserSeeder.DEMO_USERNAME)
                .param("password", DemoUserSeeder.DEMO_PASSWORD);
        if (rememberMe) {
            request.param("remember-me", "true");
        }
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void anonymousRequestReportsAnonymousLevel() throws Exception {
        // UC-007 main scenario step 2 / Test Adimi 1's first half.
        mockMvc.perform(get("/api/auth-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("ANONYMOUS"));
    }

    @Test
    void formLoginReportsFullyAuthenticatedLevel() throws Exception {
        // UC-007 main scenario step 4 / Test Adimi 1's second half.
        MvcResult loginResult = login(false);
        mockMvc.perform(get("/api/auth-status")
                        .session((MockHttpSession) loginResult.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("FULLY_AUTHENTICATED"));
    }

    @Test
    void rememberMeCookieAloneReportsRememberedLevel() throws Exception {
        // UC-007 main scenario step 6 / Test Adimi 2 (session gone, only the
        // remember-me cookie authenticates).
        MvcResult loginResult = login(true);
        Cookie rememberMe = loginResult.getResponse().getCookie("remember-me");
        assertThat(rememberMe).isNotNull();

        mockMvc.perform(get("/api/auth-status").cookie(rememberMe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("REMEMBERED"));
    }

    @Test
    void rememberedSessionIsRejectedFromAccountSettingsByTheBackendItself() throws Exception {
        // BR-010/BR-011, Test Adimi 5 first half: this is the backend
        // enforcement itself (isFullyAuthenticated() in SecurityConfig), not
        // a check that only the frontend route guard would catch.
        MvcResult loginResult = login(true);
        Cookie rememberMe = loginResult.getResponse().getCookie("remember-me");
        assertThat(rememberMe).isNotNull();

        mockMvc.perform(get("/api/account").cookie(rememberMe))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullyAuthenticatedSessionCanReachAccountSettings() throws Exception {
        MvcResult loginResult = login(false);
        mockMvc.perform(get("/api/account")
                        .session((MockHttpSession) loginResult.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(DemoUserSeeder.DEMO_USERNAME));
    }

    @Test
    void anonymousRequestToAccountSettingsIsRejectedToo() throws Exception {
        // UC-008 A2.
        mockMvc.perform(get("/api/account"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void correctPasswordUpgradesRememberedToFullyAuthenticatedAndUnlocksAccountSettings() throws Exception {
        // UC-009 main scenario, and Test Adimi 5 second half: rejected
        // before re-auth, 200 after, for the same underlying flow.
        MvcResult loginResult = login(true);
        Cookie rememberMe = loginResult.getResponse().getCookie("remember-me");
        assertThat(rememberMe).isNotNull();

        mockMvc.perform(get("/api/account").cookie(rememberMe))
                .andExpect(status().isUnauthorized());

        MvcResult reauthResult = mockMvc.perform(post("/api/reauthenticate")
                        .cookie(rememberMe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + DemoUserSeeder.DEMO_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("FULLY_AUTHENTICATED"))
                .andReturn();

        // Proves the upgrade actually landed in a persisted session, not
        // just in the SecurityContextHolder for the duration of that one
        // request - see ReauthenticationController's javadoc.
        MockHttpSession upgradedSession = (MockHttpSession) reauthResult.getRequest().getSession(false);
        assertThat(upgradedSession).isNotNull();

        mockMvc.perform(get("/api/auth-status").session(upgradedSession))
                .andExpect(jsonPath("$.level").value("FULLY_AUTHENTICATED"));
        mockMvc.perform(get("/api/account").session(upgradedSession))
                .andExpect(status().isOk());
    }

    @Test
    void a1WrongPasswordLeavesTheLevelAsRememberedAndAccountSettingsStillBlocked() throws Exception {
        MvcResult loginResult = login(true);
        Cookie rememberMe = loginResult.getResponse().getCookie("remember-me");
        assertThat(rememberMe).isNotNull();

        mockMvc.perform(post("/api/reauthenticate")
                        .cookie(rememberMe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"not-the-password\"}"))
                .andExpect(status().isUnauthorized());

        // Level unchanged (A1): still Remembered off the same cookie, still
        // blocked from Account Settings.
        mockMvc.perform(get("/api/auth-status").cookie(rememberMe))
                .andExpect(jsonPath("$.level").value("REMEMBERED"));
        mockMvc.perform(get("/api/account").cookie(rememberMe))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reauthenticationIgnoresAnyUsernameAndOnlyEverChecksTheCurrentPrincipalsOwnPassword() throws Exception {
        // BR-012: ReauthenticateRequest has no username field at all - the
        // controller always resolves the account from `Authentication`, the
        // Remembered session's own principal. This test pins that down by
        // checking the field simply isn't there to send: a request body
        // that tries to smuggle one is silently ignored (Jackson drops
        // unknown properties by default), and the check still succeeds
        // against the demo account's own password.
        MvcResult loginResult = login(true);
        Cookie rememberMe = loginResult.getResponse().getCookie("remember-me");
        assertThat(rememberMe).isNotNull();

        mockMvc.perform(post("/api/reauthenticate")
                        .cookie(rememberMe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"someone-else\",\"password\":\""
                                + DemoUserSeeder.DEMO_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("FULLY_AUTHENTICATED"));
    }

}
