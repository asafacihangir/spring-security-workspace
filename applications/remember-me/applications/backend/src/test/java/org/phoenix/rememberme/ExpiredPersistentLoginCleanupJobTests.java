package org.phoenix.rememberme;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * Faz 8 (UC-014 Test Adimi 4, BR-020): proves
 * {@link ExpiredPersistentLoginCleanupJob#deleteExpiredRows()} deletes
 * exactly the rows whose {@code last_used + tokenValiditySeconds < now} and
 * nothing else - the job is invoked directly rather than waiting on
 * {@code @Scheduled}'s own timer (see {@code cleanup-interval-ms} override
 * below, set far longer than any test could run, so the background
 * scheduler never interferes with these assertions), matching how
 * {@link RememberMeAutoLoginAndExpiryTests} avoids sleep-based flakiness
 * elsewhere in this suite.
 *
 * <p>{@code token-validity-seconds=5} keeps "expired" vs. "still valid" easy
 * to construct precisely with hand-picked {@code last_used} timestamps
 * rather than needing to wait out a real interval.
 *
 * <p>See {@link ExpiredPersistentLoginCleanupJobErrorHandlingTests} for
 * BR-021/A1 (DB erisilemez) - that one is a pure unit test against a mocked
 * {@link JdbcTemplate}, not this real-database class, since actually taking
 * MySQL down is exercised manually per task-8-report.md, not from an
 * automated test.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "app.remember-me.token-validity-seconds=5",
        "app.remember-me.cleanup-interval-ms=999999999"
})
class ExpiredPersistentLoginCleanupJobTests {

    private static final String INSERT_SQL =
            "insert into persistent_logins (series, username, token, last_used) values (?, ?, ?, ?)";

    @Autowired
    private ExpiredPersistentLoginCleanupJob cleanupJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteRowsCreatedDuringTest() {
        jdbcTemplate.update("delete from persistent_logins where username = ?", DemoUserSeeder.DEMO_USERNAME);
    }

    private void insertRow(String series, Instant lastUsed) {
        jdbcTemplate.update(INSERT_SQL, series, DemoUserSeeder.DEMO_USERNAME, "some-token", Timestamp.from(lastUsed));
    }

    private boolean rowExists(String series) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from persistent_logins where series = ?", Long.class, series);
        return count != null && count > 0;
    }

    @Test
    void br020_deletesOnlyTheExpiredRowAndLeavesTheStillValidRowInPlace() {
        // token-validity-seconds=5: well past that counts as expired, well
        // within it counts as still valid.
        insertRow("expired-series", Instant.now().minusSeconds(60));
        insertRow("valid-series", Instant.now());

        cleanupJob.deleteExpiredRows();

        assertThat(rowExists("expired-series")).as("expired row must be deleted").isFalse();
        assertThat(rowExists("valid-series")).as("still-valid row must survive").isTrue();
    }

    @Test
    void a2_noExpiredRowsIsANoOpThatDoesNotTouchValidRows() {
        insertRow("valid-series-a2", Instant.now());

        cleanupJob.deleteExpiredRows();

        assertThat(rowExists("valid-series-a2")).isTrue();
    }

}
