package org.phoenix.rememberme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Faz 4 (UC-004, UC-005): auto-login after session loss, and rejection of an
 * expired remember-me cookie.
 *
 * <p>{@code TokenBasedRememberMeServices}, once wired via {@code rememberMe()}
 * (Faz 3), already performs the auto-login itself - there is no new
 * remember-me machinery to build in this phase. What this class actually
 * pins down:
 *
 * <ul>
 * <li>{@link #rememberMeAutoLoginProducesARememberMeAuthenticationTokenNotAFullLogin()}
 * - Checkpoint 4's second item / BR-006: the {@code Authentication} produced
 * by remember-me auto-login is Spring Security's
 * {@code RememberMeAuthenticationToken}, a type distinct from the
 * {@code UsernamePasswordAuthenticationToken} a real form login produces.
 * This is the primitive Faz 5's Anonymous/Remembered/Fully Authenticated
 * distinction is built on top of - confirming it exists and behaves
 * correctly here, not building the distinction's UI/enforcement (that stays
 * out of scope for this phase).
 * <li>{@link #expiredRememberMeCookieIsRejectedEvenThoughItsSignatureIsValid()}
 * - BR-007: an expired cookie is never accepted, full stop. Rather than
 * shortening {@code app.remember-me.token-validity-seconds} and sleeping
 * past it (slow, and flaky under CI scheduling jitter), this test crafts a
 * cookie whose {@code expiryTime} token already lies in the past directly,
 * signing it the exact same way {@code TokenBasedRememberMeServices} does
 * (see {@code AbstractRememberMeServices}/{@code TokenBasedRememberMeServices}
 * source: {@code username:expiryTime:algorithmName:signature}, base64'd,
 * signature = hex(SHA-256(username:expiryTime:password:key))). That proves
 * the filter rejects an actually-expired-but-otherwise-validly-signed token
 * on its merits, not just "some cookie that happens to be wrong". Test
 * Adimi 1-3 (DevTools: delete JSESSIONID, wait past a short configured
 * validity, etc.) cover the end-to-end manual path this in-process test
 * can't.
 * </ul>
 *
 * <p>Runs against the real MySQL instance (task infra:up), same as the rest
 * of this suite; logs in as the demo account seeded by
 * {@link DemoUserSeeder}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RememberMeAutoLoginAndExpiryTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.remember-me.key}")
    private String rememberMeKey;

    private MvcResult login(boolean rememberMe) throws Exception {
        var request = post("/api/login")
                .param("username", DemoUserSeeder.DEMO_USERNAME)
                .param("password", DemoUserSeeder.DEMO_PASSWORD);
        if (rememberMe) {
            request.param("keep-me", "true");
        }
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void rememberMeAutoLoginProducesARememberMeAuthenticationTokenNotAFullLogin() throws Exception {
        // A real form login, session-carried: produces a full-authentication
        // token, not a remember-me one. (SecurityMockMvcResultMatchers'
        // authenticated() reads the SecurityContext back out of the
        // request-attribute/session-backed SecurityContextRepository, not
        // SecurityContextHolder - the latter is already cleared by the time
        // perform()/andExpect() returns control here, so it can't be read
        // via request.getUserPrincipal() after the fact.)
        MvcResult formLoginResult = login(false);
        mockMvc.perform(get("/api/me")
                        .session((MockHttpSession) formLoginResult.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(authenticated().withAuthentication(
                        auth -> assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class)));

        // Auto-login off the remember-me cookie alone, no session at all
        // (main scenario steps 2-4): must be a RememberMeAuthenticationToken,
        // never mistaken for a fully-authenticated session (BR-006 - a
        // "Remembered" level must not grant full-auth-only access).
        MvcResult rememberMeLoginResult = login(true);
        Cookie rememberMe = rememberMeLoginResult.getResponse().getCookie("notes-rm");
        assertThat(rememberMe).isNotNull();

        mockMvc.perform(get("/api/me").cookie(rememberMe))
                .andExpect(status().isOk())
                .andExpect(authenticated().withAuthentication(auth -> {
                    assertThat(auth).isInstanceOf(RememberMeAuthenticationToken.class);
                    assertThat(auth).isNotInstanceOf(UsernamePasswordAuthenticationToken.class);
                }));
    }

    @Test
    void expiredRememberMeCookieIsRejectedEvenThoughItsSignatureIsValid() throws Exception {
        String username = DemoUserSeeder.DEMO_USERNAME;
        String passwordHash = userRepository.findByUsername(username).orElseThrow().getPassword();
        long expiredExpiryTime = System.currentTimeMillis() - Duration.ofMinutes(1).toMillis();

        Cookie expired = new Cookie("notes-rm",
                craftTokenBasedRememberMeCookieValue(username, passwordHash, rememberMeKey, expiredExpiryTime));

        // BR-007: rejected outright - no session is granted off the back of
        // it, so the protected endpoint responds exactly as it would to no
        // cookie at all.
        mockMvc.perform(get("/api/me").cookie(expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sanityCheckACorrectlySignedNotYetExpiredCraftedCookieIsAccepted() throws Exception {
        // Companion to the expiry test above: proves the crafted-cookie
        // construction itself is faithful to TokenBasedRememberMeServices'
        // format (not that the test is accidentally rejecting everything).
        String username = DemoUserSeeder.DEMO_USERNAME;
        String passwordHash = userRepository.findByUsername(username).orElseThrow().getPassword();
        long farFutureExpiryTime = System.currentTimeMillis() + Duration.ofDays(1).toMillis();

        Cookie stillValid = new Cookie("notes-rm",
                craftTokenBasedRememberMeCookieValue(username, passwordHash, rememberMeKey, farFutureExpiryTime));

        mockMvc.perform(get("/api/me").cookie(stillValid))
                .andExpect(status().isOk());
    }

    /**
     * Reproduces {@code TokenBasedRememberMeServices}' cookie encoding
     * exactly (see {@code AbstractRememberMeServices.encodeCookie} and
     * {@code TokenBasedRememberMeServices.makeTokenSignature}): four
     * {@code ":"}-delimited, URL-encoded tokens
     * ({@code username, expiryTime, algorithmName, signature}), then
     * base64'd with trailing {@code '='} padding stripped. The signature is
     * {@code hex(SHA-256(username:expiryTime:password:key))} - the same
     * key configured via {@code app.remember-me.key} and the same
     * BCrypt hash actually stored for the account, so only the
     * {@code expiryTime} differs from what a real login would produce.
     */
    private static String craftTokenBasedRememberMeCookieValue(String username, String passwordHash, String key,
            long expiryTime) throws Exception {
        String signedData = username + ":" + expiryTime + ":" + passwordHash + ":" + key;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String signature = new String(Hex.encode(digest.digest(signedData.getBytes(StandardCharsets.UTF_8))));

        String[] tokens = { username, Long.toString(expiryTime), "SHA256", signature };
        StringBuilder plain = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            plain.append(URLEncoder.encode(tokens[i], StandardCharsets.UTF_8));
            if (i < tokens.length - 1) {
                plain.append(":");
            }
        }
        String base64 = Base64.getEncoder().encodeToString(plain.toString().getBytes(StandardCharsets.UTF_8));
        while (base64.endsWith("=")) {
            base64 = base64.substring(0, base64.length() - 1);
        }
        return base64;
    }

}
