package org.phoenix.rememberme;

import java.time.Instant;

/**
 * Faz 7 (UC-013, FR-013): one row of the {@code persistent_logins} table, as
 * returned by {@link TokenInspectorController}. Field names/order mirror
 * that table's columns - see {@link PersistentLogin}'s javadoc for how the
 * schema itself came to be, and {@link TokenInspectorController} for how
 * these rows actually get read.
 *
 * <p><b>Faz 10 (UC-017, FR-017, BR-024): {@link #boundIp}.</b> {@code null}
 * whenever the record has no bound IP for any reason - IP-binding was
 * disabled when this record was created, or it predates the feature
 * entirely (UC-017 A1). Deliberately never coerced to an empty string or
 * omitted: the frontend is the one place that turns this {@code null} into
 * an explicit, unambiguous marker ("IP'ye bağlı değil") rather than a blank
 * table cell - see {@code TokenInspectorPage.jsx}. This record itself just
 * carries the raw nullable value through, the same way {@code lastUsed}
 * etc. do.
 */
record TokenInspectorRecord(String username, String series, String token, Instant lastUsed, String boundIp) {
}
