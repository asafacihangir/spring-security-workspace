package org.phoenix.rememberme;

import java.util.List;

/**
 * Faz 7 (UC-013): response shape for {@code GET /api/token-inspector}.
 *
 * <p>{@code strategy} carries which {@link RememberMeStrategy} is currently
 * active so the frontend can tell UC-013's two "nothing to show" cases
 * apart without guessing from list emptiness alone:
 * <ul>
 * <li>A1 - persistent mode active, but no record exists yet
 * ({@code strategy == "PERSISTENT"}, {@code records} empty).
 * <li>A2 - token-based mode active, so there is no server-side record to
 * have at all ({@code strategy == "TOKEN"}, {@code records} always empty).
 * </ul>
 * Both would otherwise render as the same bare empty table, which is
 * exactly the confusing state A2 calls out.
 */
record TokenInspectorResponse(String strategy, List<TokenInspectorRecord> records) {
}
