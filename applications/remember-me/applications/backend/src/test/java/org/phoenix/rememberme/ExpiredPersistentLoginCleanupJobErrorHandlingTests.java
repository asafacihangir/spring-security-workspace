package org.phoenix.rememberme;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Faz 8 (UC-014 Test Adimi 4, A1, BR-021): pure unit test - no Spring
 * context, no MySQL - proving
 * {@link ExpiredPersistentLoginCleanupJob#deleteExpiredRows()} itself never
 * lets an exception escape, mirroring
 * {@link CookieTheftExceptionTranslationFilterTests}' approach of isolating
 * the try/catch mechanism from the full application. A mocked
 * {@link JdbcTemplate} stands in for "MySQL is unreachable"
 * ({@link DataAccessResourceFailureException} is exactly what Spring's JDBC
 * stack throws for a dropped connection), so this test is deterministic and
 * fast where actually stopping the MySQL container (task-8-report.md's
 * manual walkthrough) is neither.
 */
class ExpiredPersistentLoginCleanupJobErrorHandlingTests {

    @Test
    void aDatabaseFailureIsCaughtAndLoggedRatherThanPropagated() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(Object[].class)))
                .thenThrow(new DataAccessResourceFailureException("simulated: MySQL unreachable"));
        ExpiredPersistentLoginCleanupJob job = new ExpiredPersistentLoginCleanupJob(jdbcTemplate, 5);

        // Must not rethrow - letting a DB failure escape this method is
        // exactly the bug BR-021 forbids (it would otherwise kill the
        // scheduler/app in ways requiring a restart).
        job.deleteExpiredRows();

        verify(jdbcTemplate, times(1)).update(anyString(), any(Object[].class));
    }

}
