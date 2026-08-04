package org.phoenix.rememberme;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;

/**
 * Faz 10 (UC-016, UC-017, FR-016/FR-017, BR-023/BR-024): the one custom
 * piece of security logic in this app that has no Spring Security default
 * to lean on - {@code PersistentTokenBasedRememberMeServices} has no notion
 * of "client IP" at all. This subclass adds exactly two things on top of
 * it: recording the client IP a persistent login token was created from
 * ({@link #onLoginSuccess}), and rejecting an auto-login attempt whose
 * request IP does not match that recorded IP ({@link #processAutoLoginCookie}).
 * Everything else - series/token generation, rotation, expiry, theft
 * detection (BR-015/016/017/018, Faz 6/7) - is untouched, either by not
 * overriding it at all ({@code processAutoLoginCookie} still delegates to
 * {@code super} for all of that) or by mirroring the parent's own logic
 * exactly ({@code onLoginSuccess} - see its javadoc for why it is a mirror
 * rather than a call to {@code super}).
 *
 * <p><b>Scope decision: persistent strategy only.</b> IP-binding only makes
 * sense here because {@code persistent_logins} already gives this strategy
 * a server-side record to attach an IP to and check against on every
 * auto-login. Faz 3/4's stateless, HMAC-signed {@code TokenBasedRememberMeServices}
 * has no server-side record of any kind - binding an IP to it would mean
 * embedding the IP inside the cookie's signed payload, a materially larger
 * change this phase's brief does not ask for. {@link SecurityConfig} only
 * ever constructs this class when {@code app.remember-me.strategy=persistent}
 * (see its {@code filterChain} javadoc "Faz 10" note); the {@code token}
 * strategy branch never touches this class at all, so its behavior is
 * exactly Faz 3/4/9's, unchanged - the regression check Checkpoint 10 asks
 * for.
 *
 * <p><b>IP source: {@code HttpServletRequest.getRemoteAddr()}.</b> Not
 * {@code X-Forwarded-For} or any other proxy header - {@code infra.yml} (the
 * only infrastructure this app defines) runs nothing but MySQL, and the
 * backend/frontend both run directly on the host with no reverse proxy or
 * load balancer of any kind between a caller and this servlet container, so
 * there is no trusted intermediary rewriting that header; trusting it here
 * would let any caller simply claim whatever IP it likes and defeat the
 * whole point of binding. ({@code docs/vision.md}'s own UC-016 "Independent
 * Test" suggestion floats {@code X-Forwarded-For} spoofing as one way to
 * simulate a different IP - that only works if this class actually reads
 * and trusts that header, which it deliberately does not; see the README's
 * "IP-Bound Remember-Me" section for what actually works for manual testing
 * against this {@code getRemoteAddr()}-only design instead.)
 * {@code getRemoteAddr()} is the actual TCP peer address the servlet
 * container observed - not spoofable by the client - which is exactly what
 * BR-023 needs "the IP the record was produced from" to mean.
 *
 * <p><b>Feature toggle: {@code app.remember-me.ip-binding-enabled}.</b>
 * {@code ipBindingEnabled} gates both halves of this class's added
 * behavior identically: when {@code false}, {@link #onLoginSuccess} never
 * writes {@code bound_ip} (new records read back as "not IP-bound" - see
 * {@link TokenInspectorController}, UC-017 A1) and
 * {@link #processAutoLoginCookie} never even queries it, let alone rejects
 * on it (UC-016 A2/BR-023's "only when binding is active" scope). A record
 * written while binding was enabled keeps its {@code bound_ip} if binding
 * is later disabled - {@link #processAutoLoginCookie}'s enforcement is
 * still off in that case, purely because the toggle itself is off, not
 * because the column happens to be empty. This mirrors
 * {@link RememberMeStrategy}'s own "one property, no half-states" shape.
 */
final class IpBoundPersistentTokenBasedRememberMeServices extends PersistentTokenBasedRememberMeServices {

    private static final Logger log = LoggerFactory.getLogger(IpBoundPersistentTokenBasedRememberMeServices.class);

    private static final String UPDATE_BOUND_IP_SQL = "update persistent_logins set bound_ip = ? where series = ?";
    private static final String SELECT_BOUND_IP_SQL = "select bound_ip from persistent_logins where series = ?";

    // Faz 10: kept as its own field (constructed from the exact same
    // instance passed to super's constructor) rather than reached for via
    // reflection into the superclass's own private tokenRepository field -
    // see onLoginSuccess's javadoc for why this class needs direct access
    // to it at all.
    private final PersistentTokenRepository tokenRepository;
    private final JdbcTemplate jdbcTemplate;
    private final boolean ipBindingEnabled;

    IpBoundPersistentTokenBasedRememberMeServices(String key, UserDetailsService userDetailsService,
            PersistentTokenRepository tokenRepository, JdbcTemplate jdbcTemplate, boolean ipBindingEnabled) {
        super(key, userDetailsService, tokenRepository);
        this.tokenRepository = tokenRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.ipBindingEnabled = ipBindingEnabled;
    }

    /**
     * Faz 10 (UC-016 main scenario step 2, FR-016): records the client IP a
     * brand new persistent login token was created from.
     *
     * <p><b>Why this mirrors {@code PersistentTokenBasedRememberMeServices.onLoginSuccess}
     * instead of calling {@code super.onLoginSuccess(...)} and reading the
     * series back out some other way:</b> the parent implementation
     * generates the series/token pair, persists it, and sets the cookie -
     * all inside one {@code private} method with no return value and no
     * hook for a subclass to learn which series it just created. Rather
     * than smuggle that value out via a {@code ThreadLocal} wrapped around
     * an override of the (also parent-owned) {@code generateSeriesData()},
     * this method reimplements the same four steps directly, using only
     * members {@code AbstractRememberMeServices}/{@code PersistentTokenBasedRememberMeServices}
     * already expose as {@code protected} ({@code generateSeriesData()},
     * {@code generateTokenData()}, {@code getTokenValiditySeconds()},
     * {@code setCookie(...)}) plus this class's own {@link #tokenRepository}
     * field (the very same instance handed to {@code super}'s constructor -
     * see the constructor). This is the same "mirror the library's own
     * logic in a small helper, since it isn't independently reusable"
     * pattern this codebase already uses for
     * {@link PersistentCookieCodec} (test support mirroring
     * {@code AbstractRememberMeServices#decodeCookie}) - the algorithm
     * mirrored here is equally small and has been stable since Spring
     * Security 2.0.
     *
     * <p>The IP write is a separate {@code UPDATE} issued only after the
     * token row itself is safely created - functionally equivalent to
     * writing it as part of one combined statement, but keeps this method
     * from having to duplicate {@code JdbcTokenRepositoryImpl}'s own INSERT
     * SQL as well (which remains entirely Spring Security's to own - see
     * {@link PersistentLogin}'s javadoc).
     */
    @Override
    protected void onLoginSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication successfulAuthentication) {
        String username = successfulAuthentication.getName();
        String series = generateSeriesData();
        String tokenValue = generateTokenData();
        PersistentRememberMeToken persistentToken = new PersistentRememberMeToken(username, series, tokenValue,
                new Date());
        try {
            tokenRepository.createNewToken(persistentToken);
            setCookie(new String[] { series, tokenValue }, getTokenValiditySeconds(), request, response);
        } catch (Exception ex) {
            log.error("Failed to save persistent token ", ex);
            return;
        }
        if (ipBindingEnabled) {
            // Faz 10 (UC-016 A2): only recorded when the toggle is on - see
            // class javadoc "Feature toggle" section for why this, not "always
            // record but only enforce conditionally", is the chosen behavior.
            String clientIp = request.getRemoteAddr();
            jdbcTemplate.update(UPDATE_BOUND_IP_SQL, clientIp, series);
        }
    }

    /**
     * Faz 10 (UC-016 main scenario steps 4-6, A1, BR-023): rejects an
     * auto-login attempt whose request IP does not match the IP the
     * presented series was bound to, before delegating to
     * {@code super.processAutoLoginCookie(...)} for everything else
     * (series/token match, rotation, expiry, theft detection - all
     * untouched).
     *
     * <p><b>Ordering: checked before, not after, {@code super}'s own
     * validation - and the check never mutates anything.</b> The
     * alternative (call {@code super.processAutoLoginCookie(...)} first,
     * then reject on IP mismatch afterward) was considered and rejected:
     * by the time {@code super} returns, it has already rotated the
     * token and written the new value both to {@code persistent_logins}
     * and to the response's {@code Set-Cookie} header (BR-015) - even for
     * a request this method is about to reject. That would silently
     * invalidate the legitimate owner's own still-valid cookie (its
     * series now points at a token value that was only ever handed to the
     * wrong-IP caller) the moment anyone - attacker or not - replays a
     * copied cookie from a different network, turning a clean "reject this
     * one request" into a denial-of-service against the real owner's next
     * genuine auto-login (which would then look like stale-token replay
     * and trigger Spring's own full theft wipe, BR-017/018). Checking
     * first avoids all of that: a rejected attempt here never calls
     * {@code tokenRepository.updateToken(...)}, so the stored row and the
     * legitimate owner's own cookie are left exactly as they were.
     *
     * <p>One consequence of checking first: a request presenting the right
     * series but a stale/wrong token *and* the wrong IP is rejected here,
     * on the IP check alone, without ever reaching {@code super}'s own
     * token-mismatch/theft-wipe logic. This is intentional, not a
     * weakening - IP-binding is an additional, stricter gate layered in
     * front of the existing series/token check, not a replacement for it:
     * any request from an unbound IP is refused outright, and every
     * request from the bound IP still goes through {@code super}'s full,
     * unmodified series/token/theft validation exactly as before.
     *
     * <p>A missing {@code bound_ip} - no row at all for the series (e.g. an
     * unrecognized/garbage cookie), or a row whose {@code bound_ip} is
     * {@code null} (binding was off when the record was created, or the
     * record predates this feature) - is treated leniently: no IP check is
     * performed, and control falls through to {@code super}, which handles
     * "series not found" (or any other outcome) exactly as it always has.
     * This is what makes UC-016 A2/BR-023's "only when binding is enabled"
     * scope hold even for old records, without this class needing to know
     * anything about when a given row was created.
     */
    @Override
    protected UserDetails processAutoLoginCookie(String[] cookieTokens, HttpServletRequest request,
            HttpServletResponse response) {
        if (ipBindingEnabled && cookieTokens.length == 2) {
            String presentedSeries = cookieTokens[0];
            String boundIp = findBoundIp(presentedSeries);
            String requestIp = request.getRemoteAddr();
            if (boundIp != null && !boundIp.equals(requestIp)) {
                // BR-023: a clean rejection, same shape as any other invalid
                // remember-me attempt - AbstractRememberMeServices.autoLogin's
                // catch(RememberMeAuthenticationException) logs, cancels the
                // cookie, and returns null (falls through to the normal
                // "not authenticated" flow). Deliberately NOT
                // CookieTheftException - that type triggers Spring's own
                // remove-every-series-for-this-user wipe (BR-018), which is
                // not what a plain IP mismatch should do (see class javadoc
                // "Scope decision" and this method's own "Ordering" note).
                log.debug("Remember-me auto-login rejected: series '{}' is bound to IP '{}', request IP was '{}'",
                        presentedSeries, boundIp, requestIp);
                throw new RememberMeAuthenticationException(
                        "Remember-me cookie is bound to a different IP address");
            }
        }
        return super.processAutoLoginCookie(cookieTokens, request, response);
    }

    private String findBoundIp(String series) {
        List<String> rows = jdbcTemplate.query(SELECT_BOUND_IP_SQL, (rs, rowNum) -> rs.getString("bound_ip"),
                series);
        return rows.isEmpty() ? null : rows.get(0);
    }

}
