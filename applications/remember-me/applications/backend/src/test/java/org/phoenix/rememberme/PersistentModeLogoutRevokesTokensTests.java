package org.phoenix.rememberme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Final-review fix-round (finding 4): proves, with an actual assertion
 * instead of just prose, the claim {@link RememberMeAndLogoutTests}'s
 * {@code knownLimitationAReplayedPreLogoutRememberMeCookieValueStillAuthenticates}
 * javadoc makes about token-mode's revocation gap - that
 * {@code app.remember-me.strategy=persistent} (Faz 6) closes it. Before this
 * class existed, nothing in this suite actually logged in under persistent
 * mode, logged out, and then replayed the pre-logout cookie to check it was
 * rejected; the "persistent mode closes the gap" claim lived only in
 * comments.
 *
 * <p>Mirrors {@link RememberMeAndLogoutTests}'s structure (same demo user,
 * same {@code notes-rm} cookie name) but runs against its own
 * {@code @TestPropertySource}-scoped context with
 * {@code app.remember-me.strategy=persistent}, the same convention
 * {@link PersistentRememberMeStrategyTests} and
 * {@link RememberMeStrategyDoesNotWriteToPersistentLoginsTests} already use
 * to keep the two strategies from ever sharing one running context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.remember-me.strategy=persistent")
class PersistentModeLogoutRevokesTokensTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteRowsCreatedDuringTest() {
        jdbcTemplate.update("delete from persistent_logins where username = ?", DemoUserSeeder.DEMO_USERNAME);
    }

    private long countPersistentLoginsForDemoUser() {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from persistent_logins where username = ?",
                Long.class, DemoUserSeeder.DEMO_USERNAME);
        return count == null ? 0 : count;
    }

    @Test
    void logoutDeletesThePersistentLoginRowAndTheReplayedPreLogoutCookieNoLongerAuthenticates() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/login").with(csrf())
                        .param("username", DemoUserSeeder.DEMO_USERNAME)
                        .param("password", DemoUserSeeder.DEMO_PASSWORD)
                        .param("keep-me", "true"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie rememberMe = loginResult.getResponse().getCookie("notes-rm");
        assertThat(rememberMe).isNotNull();
        assertThat(countPersistentLoginsForDemoUser())
                .as("persistent-mode login must write a row")
                .isEqualTo(1);

        // Deliberately carries the SESSION from login, not just the cookie:
        // a real browser always has both after a normal form login, and
        // PersistentTokenBasedRememberMeServices.logout(...) only calls
        // tokenRepository.removeUserTokens(...) when
        // SecurityContextHolder's current Authentication is non-null at the
        // moment LogoutFilter runs its handlers - which, for a cookie-only
        // request (no session), it is NOT yet, since RememberMeAuthenticationFilter
        // (the filter that would turn the cookie into an Authentication) runs
        // later in the default filter order than LogoutFilter does. A
        // cookie-only logout call here would exercise that (real, separate)
        // ordering quirk instead of the revocation behavior this test exists
        // to prove.
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mockMvc.perform(post("/api/logout").with(csrf()).session(session).cookie(rememberMe))
                .andExpect(status().isOk());

        // The server-side half of the claim: unlike token-mode (stateless,
        // nothing to delete), logout here actually deletes the
        // persistent_logins row for this series/user.
        assertThat(countPersistentLoginsForDemoUser())
                .as("logout under persistent mode must delete the persistent_logins row")
                .isZero();

        // The replay half: the exact pre-logout cookie value, resubmitted
        // after logout, must no longer authenticate anything - unlike
        // RememberMeAndLogoutTests.knownLimitationAReplayedPreLogoutRememberMeCookieValueStillAuthenticates,
        // which documents that token-mode's equivalent replay DOES still
        // succeed. This is the concrete difference persistent mode buys.
        mockMvc.perform(get("/api/me").cookie(rememberMe))
                .andExpect(status().isUnauthorized());
    }

}
