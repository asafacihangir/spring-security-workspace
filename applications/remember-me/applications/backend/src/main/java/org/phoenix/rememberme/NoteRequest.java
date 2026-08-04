package org.phoenix.rememberme;

/**
 * Request body for creating/updating a note (UC-006 steps 3-5, A1). Carries
 * raw client input only - {@link NoteController} validates {@code title}
 * server-side (A3) rather than trusting whatever the frontend already
 * checked.
 */
public record NoteRequest(String title, String content) {
}
