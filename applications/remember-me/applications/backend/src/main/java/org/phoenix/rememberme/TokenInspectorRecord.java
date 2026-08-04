package org.phoenix.rememberme;

import java.time.Instant;

/**
 * Faz 7 (UC-013, FR-013): one row of the {@code persistent_logins} table, as
 * returned by {@link TokenInspectorController}. Field names/order mirror
 * that table's columns - see {@link PersistentLogin}'s javadoc for how the
 * schema itself came to be, and {@link TokenInspectorController} for how
 * these rows actually get read.
 */
record TokenInspectorRecord(String username, String series, String token, Instant lastUsed) {
}
