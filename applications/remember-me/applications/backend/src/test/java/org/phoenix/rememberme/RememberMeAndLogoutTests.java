package org.phoenix.rememberme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Test Adimi 4: automated coverage for UC-002's opt-in remember-me cookie
 * (BR-003, BR-004/NFR-001) and UC-003's logout flow, including A1 (cookies
 * still get cleared even when the server-side session is already gone) and
 * BR-005 (neither cookie works for anything after logout).
 *
 * <p>Runs against the real MySQL instance (task infra:up), same as
 * {@link FormLoginTests} and {@link NoteControllerTests}, and logs in as
 * the demo account seeded by {@link DemoUserSeeder}.
 *
 * <p>Note on {@code MockMvc} and the session cookie: unlike a real servlet
 * container, {@code MockMvc}'s mock response never synthesizes a
 * {@code Set-Cookie: JSESSIONID=...} header - session propagation across
 * separate {@code perform()} calls is done here the same way the rest of
 * this test suite does it ({@link NoteControllerTests}), by carrying the
 * {@link MockHttpSession} object forward, not a cookie string. The
 * remember-me cookie, by contrast, genuinely is emitted as a normal
 * {@code Set-Cookie} header (it is not servlet-container session magic),
 * so it is exercised here as a real {@link Cookie}. Test Adimi 1-3
 * (DevTools inspection of the real `Set-Cookie: JSESSIONID=...` header)
 * cover what this in-process test cannot.
 *
 * <p>Note on BR-005 and the remember-me cookie specifically:
 * {@code TokenBasedRememberMeServices} is stateless (no server-side token
 * store), so logout cannot cryptographically revoke an already-issued
 * token - only expire it client-side via {@code Set-Cookie}. See
 * {@link #knownLimitationAReplayedPreLogoutRememberMeCookieValueStillAuthenticates()}
 * for the empirical proof and the reasoning; that gap is what Faz 6's
 * persistent/database-backed remember-me exists to close.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RememberMeAndLogoutTests {

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
    void uncheckedLoginEstablishesASessionButProducesNoRememberMeCookie() throws Exception {
        MvcResult result = login(false);

        // A session was genuinely established (main scenario step 6)...
        assertThat(result.getRequest().getSession(false)).isNotNull();
        // ...but BR-003 (opt-in only): no remember-me cookie, not even an
        // empty/expired one, when the checkbox wasn't checked.
        assertThat(result.getResponse().getCookie("remember-me")).isNull();
    }

    @Test
    void checkedLoginProducesAnHttpOnlyRememberMeCookie() throws Exception {
        MvcResult result = login(true);

        Cookie rememberMe = result.getResponse().getCookie("remember-me");

        assertThat(rememberMe).isNotNull();
        // BR-004/NFR-001: not readable by client-side scripts. (JSESSIONID's
        // HttpOnly flag is covered separately in application.properties and
        // by Test Adimi 1-3's manual DevTools inspection - see class javadoc
        // for why MockMvc can't observe that header itself.)
        assertThat(rememberMe.isHttpOnly()).isTrue();
    }

    @Test
    void rememberMeCookieAloneAuthenticatesWithoutAnySession() throws Exception {
        MvcResult loginResult = login(true);
        Cookie rememberMe = loginResult.getResponse().getCookie("remember-me");
        assertThat(rememberMe).isNotNull();

        // No session attached at all here - proves UC-002's postcondition
        // that the user is recognized purely off the remember-me cookie
        // once the session is gone, not just that a cookie happens to exist.
        mockMvc.perform(get("/api/me").cookie(rememberMe))
                .andExpect(status().isOk());
    }

    @Test
    void logoutInvalidatesTheSessionSoItNoLongerAuthenticates() throws Exception {
        MvcResult loginResult = login(false);
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/logout").session(session))
                .andExpect(status().isOk());

        // BR-005 (session half): the exact same session that worked above
        // no longer authenticates anything post-logout.
        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/api/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutClearsTheRememberMeCookieClientSide() throws Exception {
        MvcResult loginResult = login(true);
        Cookie rememberMe = loginResult.getResponse().getCookie("remember-me");
        assertThat(rememberMe).isNotNull();

        MvcResult logoutResult = mockMvc.perform(post("/api/logout").cookie(rememberMe))
                .andExpect(status().isOk())
                .andReturn();

        // logout()'s deleteCookies("JSESSIONID", "remember-me") plus
        // TokenBasedRememberMeServices' own LogoutHandler.logout() both
        // reply with an immediately-expiring Set-Cookie: a compliant
        // browser overwrites/drops its stored remember-me cookie right
        // here, which is what actually enforces BR-005 for this cookie in
        // practice (see the class javadoc on why JSESSIONID's equivalent
        // header isn't asserted here).
        Cookie cleared = logoutResult.getResponse().getCookie("remember-me");
        assertThat(cleared).isNotNull();
        assertThat(cleared.getMaxAge()).isZero();
    }

    @Test
    void a1LogoutStillClearsTheRememberMeCookieEvenWhenTheSessionIsAlreadyInvalidServerSide() throws Exception {
        MvcResult loginResult = login(true);
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        Cookie rememberMe = loginResult.getResponse().getCookie("remember-me");
        assertThat(session).isNotNull();
        assertThat(rememberMe).isNotNull();

        // Invalidate the session directly (not via /api/logout), simulating
        // UC-003 A1's trigger: "oturum sunucuda zaten geçersizdir" before
        // logout is even called.
        session.invalidate();

        // A1: logout must not error out or silently skip cookie clearing
        // just because the session is already gone - it still replies 200
        // and still emits a Set-Cookie that expires the remember-me cookie.
        MvcResult logoutResult = mockMvc.perform(post("/api/logout").cookie(rememberMe))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cleared = logoutResult.getResponse().getCookie("remember-me");
        assertThat(cleared).isNotNull();
        assertThat(cleared.getMaxAge()).isZero();
    }

    @Test
    void knownLimitationAReplayedPreLogoutRememberMeCookieValueStillAuthenticates() throws Exception {
        // Documents a real, deliberate gap rather than hiding it: this
        // phase uses TokenBasedRememberMeServices (stateless, HMAC-signed -
        // brief's explicit choice for Faz 3), which has no server-side
        // token store to revoke from. Logout expires the cookie in the
        // browser (see logoutClearsTheRememberMeCookieClientSide above),
        // but if an attacker captured the raw cookie value before logout
        // and replays it directly (bypassing the browser entirely), the
        // signature is still valid and this still authenticates - a strict
        // reading of BR-005 ("hiçbir mevcut cookie ile... otomatik giriş
        // yapılamaz") is NOT fully met at the protocol level for the
        // remember-me cookie specifically. Closing this gap requires
        // server-side per-token revocation, which is exactly what Faz 6's
        // persistent/database-backed remember-me adds. This test pins down
        // today's actual behavior so a future change (accidental or Faz 6's
        // deliberate fix) is a visible, reviewed decision instead of a
        // silent regression either way.
        MvcResult loginResult = login(true);
        Cookie rememberMe = loginResult.getResponse().getCookie("remember-me");
        assertThat(rememberMe).isNotNull();

        mockMvc.perform(post("/api/logout").cookie(rememberMe))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/me").cookie(rememberMe))
                .andExpect(status().isOk());
    }

    @Test
    void logoutWithNoCookiesAtAllStillSucceeds() throws Exception {
        // Even more extreme than A1: a caller with no session and no
        // remember-me cookie (never logged in, or already cleaned up by the
        // browser) hitting /api/logout must not blow up - LogoutFilter runs
        // ahead of the authorization check, so this never needs permitAll
        // to behave, but the endpoint is explicitly permitAll'd anyway.
        mockMvc.perform(post("/api/logout"))
                .andExpect(status().isOk());
    }

}
