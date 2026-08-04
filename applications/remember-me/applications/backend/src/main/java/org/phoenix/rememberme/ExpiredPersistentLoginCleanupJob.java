package org.phoenix.rememberme;

import java.sql.Timestamp;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Faz 8 (UC-014, FR-014, NFR-003): scheduled background job that deletes
 * expired {@code persistent_logins} rows so the table does not grow forever
 * (main scenario steps 3-4).
 *
 * <p><b>"Expired" (BR-020):</b> deliberately reuses the exact same
 * {@code app.remember-me.token-validity-seconds} value {@link SecurityConfig}
 * feeds into {@code PersistentTokenBasedRememberMeServices.tokenValiditySeconds(...)}
 * - there is only one notion of "how long a remember-me token is valid" in
 * this app, not a second cleanup-specific one that could drift from it. A
 * row is expired when {@code last_used + tokenValiditySeconds < now}; the
 * cutoff instant is computed once per run in application code and bound as a
 * single {@code Timestamp} parameter, so the {@code DELETE} is one
 * database-agnostic query (no MySQL-specific {@code INTERVAL} arithmetic)
 * rather than a fetch-then-loop-delete.
 *
 * <p>Reads/writes {@code persistent_logins} the same way
 * {@link TokenInspectorController} reads it: a raw SQL string over the
 * shared {@link JdbcTemplate}, not a repository - see that class's javadoc
 * for why this table has no Spring Data repository of its own. This is the
 * one place application code deletes from {@code persistent_logins} (Spring
 * Security's {@code JdbcTokenRepositoryImpl} is the one place that writes/
 * updates it during normal logins/rotations - see
 * {@link SecurityConfig#persistentTokenRepository}).
 *
 * <p><b>Interval (Test Adimi 1/3):</b> {@code app.remember-me.cleanup-interval-ms}
 * is its own config value, deliberately separate from
 * {@code token-validity-seconds}, so either can be shortened independently
 * when demoing/testing - e.g. a 5s validity with a 2s cleanup interval lets
 * Test Adimi 1 be observed end-to-end in well under a minute instead of
 * waiting out a production-sized interval.
 *
 * <p><b>BR-021 (kesintisiz hizmet) / A1 (DB erisilemez):</b> the entire body
 * runs inside its own {@code try/catch}. Spring's scheduler already isolates
 * a failing {@code @Scheduled} method - a thrown exception only skips that
 * run, it does not crash the app or stop future runs - but that default
 * handling is easy to miss in a log stream. This method catches explicitly
 * and logs at {@code ERROR} with a message that names exactly what failed,
 * so "hata dayanıklılığı: DB erişilemezse logla ve devam et" has an
 * unambiguous, intentional log line to point to (Test Adimi 3), and returns
 * normally either way - never letting an exception propagate out of this
 * method. The next scheduled run retries the same query untouched (A1 step
 * 2); no restart is ever required.
 */
@Component
public class ExpiredPersistentLoginCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiredPersistentLoginCleanupJob.class);

    private static final String DELETE_EXPIRED_SQL = "delete from persistent_logins where last_used < ?";

    private final JdbcTemplate jdbcTemplate;
    private final int tokenValiditySeconds;

    public ExpiredPersistentLoginCleanupJob(JdbcTemplate jdbcTemplate,
            @Value("${app.remember-me.token-validity-seconds}") int tokenValiditySeconds) {
        this.jdbcTemplate = jdbcTemplate;
        this.tokenValiditySeconds = tokenValiditySeconds;
    }

    @Scheduled(fixedRateString = "${app.remember-me.cleanup-interval-ms}")
    public void deleteExpiredRows() {
        try {
            Timestamp cutoff = Timestamp.from(Instant.now().minusSeconds(tokenValiditySeconds));
            int deletedCount = jdbcTemplate.update(DELETE_EXPIRED_SQL, cutoff);
            if (deletedCount > 0) {
                log.info("Expired persistent-login cleanup deleted {} row(s) older than {}", deletedCount, cutoff);
            }
            // A2 (deletedCount == 0): a normal, silent no-op - nothing to log.
        } catch (Exception ex) {
            // BR-021/A1: caught deliberately (see class javadoc) - logged and
            // swallowed, never rethrown. The app keeps serving every other
            // request, and the next scheduled run retries this same query.
            log.error("Expired persistent-login cleanup failed; will retry on the next scheduled run", ex);
        }
    }

}
