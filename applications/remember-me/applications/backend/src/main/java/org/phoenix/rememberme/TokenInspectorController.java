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
 * <p><b>Access control:</b> {@code permitAll} (see
 * {@link SecurityConfig#filterChain}), the same reasoning as
 * {@link AuthStatusController}. This is a single-demo-user teaching/
 * debugging tool with no per-caller filtering - it dumps the whole table,
 * not "the caller's own records" - so there is no confidentiality boundary
 * an auth check would actually enforce here. Requiring
 * {@code isFullyAuthenticated()} (the way {@code /api/account} does) would
 * also actively break UC-012's own walkthrough: Test Adimi 3 expects the
 * Inspector, opened in a separate tab, to keep reflecting state immediately
 * after a theft wipes the caller's own persistent login, without forcing a
 * fresh password login first just to look at what happened.
 *
 * <p><b>A1/A2:</b> distinguishing "persistent mode, no rows yet" (A1) from
 * "token mode, so there is nothing to have" (A2) needs to know which
 * {@link RememberMeStrategy} is active, not just whether the query came
 * back empty - so every response carries {@code strategy} alongside
 * {@code records}, and the query only runs at all when persistent mode is
 * active.
 */
@RestController
public class TokenInspectorController {

    private static final String SELECT_ALL_SQL =
            "select username, series, token, last_used from persistent_logins order by last_used desc";

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
                        rs.getTimestamp("last_used").toInstant()))
                : List.of();
        return new TokenInspectorResponse(strategy.name(), records);
    }

}
