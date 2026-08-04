package org.phoenix.rememberme;

/**
 * UC-009 re-authentication payload. Deliberately carries only a password,
 * no username - BR-012 requires the check to always be against the
 * CURRENTLY authenticated principal ({@link ReauthenticationController}
 * reads that from {@code Authentication}, not from this request), so there
 * is nothing here for a caller to spoof.
 */
public record ReauthenticateRequest(String password) {
}
