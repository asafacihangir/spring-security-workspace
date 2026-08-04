package org.phoenix.rememberme;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Faz 7 (UC-013, FR-013, BR-019): read-only view onto Spring Security's
 * {@code persistent_logins} table, so UC-011's rotation (BR-015/016) and
 * UC-012's theft detection (BR-017/018) can actually be observed rather than
 * taken on faith.
 *
 * <p>Reads run over plain {@link JdbcTemplate} against the exact same table
 * {@code JdbcTokenRepositoryImpl} owns - see {@link PersistentLogin}'s
 * javadoc for why that {@code @Entity} itself is never queried directly.
 * This class is the one place application code reads from
 * {@code persistent_logins}, mirroring how {@code JdbcTokenRepositoryImpl}
 * (wired in {@link SecurityConfig#persistentTokenRepository}) is the one
 * place that writes to it.
 *
 * <p><b>BR-019 (freshness):</b> no caching of any kind sits in front of the
 * query - every call runs a fresh {@code SELECT} against the database, so
 * "yenileme sonrasi en gec 2 sn icinde guncel durumu gosterir" holds
 * trivially; there is nothing here that could go stale between two calls.
 *
 * <p><b>Access control - read this before copying this pattern anywhere
 * else:</b> {@code permitAll} (see {@link SecurityConfig#filterChain}), the
 * same reasoning as {@link AuthStatusController}. This endpoint returns live
 * bearer credentials with zero authentication: {@code series} and
 * {@code token} together are not a description of the remember-me cookie,
 * they ARE it - {@code base64(series:token)} reconstructs the exact cookie
 * value {@code IpBoundPersistentTokenBasedRememberMeServices}/
 * {@code PersistentTokenBasedRememberMeServices} would accept as a valid
 * auto-login credential. Anyone who can load this page can copy that value
 * and authenticate as the demo user, no password required.
 *
 * <p>That is intentional here for exactly one reason: UC-012's stolen-cookie
 * walkthrough requires a learner to literally copy a live remember-me cookie
 * value out of a running app and replay it, and there is no way to do that
 * without a screen that shows the real, unmasked value. This is a pedagogical
 * trade-off for a single-learner local demo, not a general access-control
 * argument - earlier drafts of this javadoc justified the open endpoint as
 * "no confidentiality boundary because there's only one user," which
 * conflates tenancy (how many users the schema supports) with secrecy (what
 * an unauthenticated caller can extract); a single-tenant app can still have
 * secrets, and a live auth token is one. The actual justification is
 * narrower and less comfortable: pedagogy demanded it, so it was allowed, with
 * this warning attached.
 *
 * <p><b>No production system may expose an endpoint like this.</b> A real
 * "admin view" of session/token state would need strong authentication,
 * audience/role-scoping (an ordinary user must never see another user's - or
 * even their own live - token secrets), and would return session metadata
 * (username, series, last-used, IP) without ever putting the raw
 * {@code token} value in a response body. Requiring
 * {@code isFullyAuthenticated()} here (the way {@code /api/account} does)
 * would also still break UC-012's own walkthrough even if it were otherwise
 * desirable: Test Adimi 3 expects the Inspector, opened in a separate tab,
 * to keep reflecting state immediately after a theft wipes the caller's own
 * persistent login, without forcing a fresh password login first just to
 * look at what happened - which is one more reason this stays a deliberately
 * lab-only pattern, not a template.
 *
 * <p><b>A1/A2:</b> distinguishing "persistent mode, no rows yet" (A1) from
 * "token mode, so there is nothing to have" (A2) needs to know which
 * {@link RememberMeStrategy} is active, not just whether the query came
 * back empty - so every response carries {@code strategy} alongside
 * {@code records}, and the query only runs at all when persistent mode is
 * active.
 *
 * <p><b>Faz 10 (UC-017, FR-017, BR-024):</b> {@code bound_ip} is read
 * alongside the four columns Faz 7 already selected, straight into
 * {@link TokenInspectorRecord#boundIp} - {@code null} passes straight
 * through unchanged ({@code ResultSet.getString} on a SQL {@code NULL}
 * column returns Java {@code null}, not an empty string), and it is the
 * frontend, not this controller, that turns that {@code null} into UC-017
 * A1's explicit "not IP-bound" marker (see {@code TokenInspectorPage.jsx}).
 * No new query, no new endpoint, no strategy-flag change - this is the
 * exact same read path Faz 7 built, one column wider.
 */
@RestController
public class TokenInspectorController {

    private static final String SELECT_ALL_SQL =
            "select username, series, token, last_used, bound_ip from persistent_logins order by last_used desc";

    private final JdbcTemplate jdbcTemplate;
    private final String rememberMeStrategyProperty;

    public TokenInspectorController(JdbcTemplate jdbcTemplate,
            @Value("${app.remember-me.strategy}") String rememberMeStrategyProperty) {
        this.jdbcTemplate = jdbcTemplate;
        this.rememberMeStrategyProperty = rememberMeStrategyProperty;
    }

    @GetMapping("/api/token-inspector")
    public TokenInspectorResponse tokenInspector() {
        RememberMeStrategy strategy = RememberMeStrategy.from(rememberMeStrategyProperty);
        List<TokenInspectorRecord> records = strategy == RememberMeStrategy.PERSISTENT
                ? jdbcTemplate.query(SELECT_ALL_SQL, (rs, rowNum) -> new TokenInspectorRecord(
                        rs.getString("username"),
                        rs.getString("series"),
                        rs.getString("token"),
                        rs.getTimestamp("last_used").toInstant(),
                        rs.getString("bound_ip")))
                : List.of();
        return new TokenInspectorResponse(strategy.name(), records);
    }

}
