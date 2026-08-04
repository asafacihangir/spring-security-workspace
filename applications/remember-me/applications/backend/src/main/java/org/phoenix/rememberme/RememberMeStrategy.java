package org.phoenix.rememberme;

/**
 * Faz 6 (UC-010, BR-013/BR-014, NFR-005): the single switch between Spring
 * Security's two remember-me backing strategies. {@link #from(String)}
 * reads the raw {@code app.remember-me.strategy} property value and is the
 * one place that decides which of the two Spring Security ends up using -
 * see {@link SecurityConfig#filterChain} for where its result branches
 * between leaving {@code TokenBasedRememberMeServices} (Faz 3's stateless,
 * HMAC-signed cookie - the default when no {@code PersistentTokenRepository}
 * is attached) and switching to {@code PersistentTokenBasedRememberMeServices}
 * (database-backed, the {@code persistent_logins} table via
 * {@link SecurityConfig#persistentTokenRepository}). BR-014 holds because
 * that branch is an {@code if/else}, not a list - exactly one strategy is
 * ever attached to the one {@code RememberMeServices} Spring Security
 * builds.
 *
 * <p>{@link #from(String)} deliberately throws rather than silently
 * defaulting on an unrecognized value (UC-010 A1). It is called from inside
 * {@link SecurityConfig}'s {@code filterChain} {@code @Bean} method, a
 * singleton bean Spring instantiates eagerly during context refresh (not
 * lazily on first request), so a thrown {@link IllegalStateException} here
 * surfaces as a {@code BeanCreationException} that fails application startup
 * outright - proven empirically in
 * {@code RememberMeApplicationFailsFastOnInvalidStrategyTests}.
 */
public enum RememberMeStrategy {
    TOKEN,
    PERSISTENT;

    static RememberMeStrategy from(String propertyValue) {
        for (RememberMeStrategy strategy : values()) {
            if (strategy.name().equalsIgnoreCase(propertyValue)) {
                return strategy;
            }
        }
        throw new IllegalStateException(
                "Unrecognized app.remember-me.strategy value: '" + propertyValue
                        + "'. Expected one of: token, persistent (case-insensitive).");
    }

}
