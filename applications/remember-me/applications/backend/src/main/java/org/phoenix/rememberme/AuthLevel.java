package org.phoenix.rememberme;

import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.core.Authentication;

/**
 * UC-007's three-way Anonymous / Remembered / Fully Authenticated
 * distinction (BR-009: exactly one level is ever accurate at a time, and it
 * matches the system's real authentication state).
 *
 * <p>Deliberately not a bespoke classification: it is derived entirely from
 * what Spring Security's {@link AuthenticationTrustResolver} already knows
 * about the current {@link Authentication} -
 * <ul>
 * <li>{@code isAnonymous()} is true for the {@code AnonymousAuthenticationToken}
 * the anonymous-auth filter installs when nothing else has authenticated the
 * request;
 * <li>{@code isRememberMe()} is true for the {@code RememberMeAuthenticationToken}
 * remember-me auto-login produces (Faz 4, BR-006);
 * <li>anything else that is actually authenticated - a real form login's or
 * {@link ReauthenticationController}'s {@code UsernamePasswordAuthenticationToken} -
 * is Fully Authenticated.
 * </ul>
 * There is exactly one source of truth (the {@code Authentication} Spring
 * Security already resolved for this request); nothing here maintains its
 * own notion of "level" that could drift from it.
 */
public enum AuthLevel {
    ANONYMOUS,
    REMEMBERED,
    FULLY_AUTHENTICATED;

    public static AuthLevel of(Authentication authentication, AuthenticationTrustResolver trustResolver) {
        if (authentication == null || !authentication.isAuthenticated()
                || trustResolver.isAnonymous(authentication)) {
            return ANONYMOUS;
        }
        if (trustResolver.isRememberMe(authentication)) {
            return REMEMBERED;
        }
        return FULLY_AUTHENTICATED;
    }

}
