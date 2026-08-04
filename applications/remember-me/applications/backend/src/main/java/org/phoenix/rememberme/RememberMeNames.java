package org.phoenix.rememberme;

/**
 * Faz 9 (UC-015, BR-022): the single place that resolves the remember-me
 * request parameter name and cookie name from configuration.
 *
 * <p><b>Why a bean, not two scattered {@code @Value} defaults:</b> both
 * {@link SecurityConfig#filterChain} (which needs the resolved names to
 * configure Spring Security's {@code rememberMe()} DSL - see
 * {@code .rememberMeParameter(...)}, {@code .rememberMeCookieName(...)}, and
 * {@code .deleteCookies(...)} in that class) and
 * {@link AuthStatusController} (which needs the resolved parameter name to
 * hand to the frontend) have to agree on the exact same value. Reading
 * {@code app.remember-me.parameter-name}/{@code app.remember-me.cookie-name}
 * independently in both places - even with identical default literals -
 * would be two copies of the same fact that could drift the moment one of
 * them is edited and the other isn't. Resolving both names once, here, into
 * a bean both classes inject closes that gap: there is exactly one place in
 * the JVM where "what is the remember-me parameter name right now" is
 * decided, and everything else reads it from there.
 *
 * <p><b>Frontend side of BR-022:</b> the frontend does not hardcode a
 * parameter name at all. {@link AuthStatusController} exposes this bean's
 * {@link #parameterName()} over {@code GET /api/auth-status}, and
 * {@code LoginForm.jsx} sends its "Beni hatırla" field under whatever name
 * that response carries - so {@code application.properties} is the actual
 * single source of truth end to end, not two string literals (one Java, one
 * JS) that merely happen to agree today. The cookie name is deliberately
 * NOT exposed to the frontend: it is a pure backend/Spring-Security-DSL
 * concern (the cookie is {@code HttpOnly} and arrives via
 * {@code Set-Cookie} - the browser attaches it back automatically, and the
 * frontend never needs to name it in a request it constructs itself), so
 * only {@link #parameterName()} crosses the API boundary.
 *
 * <p><b>A1 (UC-015 - no custom name configured):</b> {@link #from} falls
 * back to Spring Security's own default, {@link #SPRING_DEFAULT}, whenever
 * a property is absent OR present-but-blank, rather than throwing or
 * producing an empty/invalid name Spring Security would reject. This app's
 * own {@code application.properties} sets both properties to this app's
 * chosen values ({@code notes-rm}/{@code keep-me} - UC-015's main scenario),
 * but the mechanism itself works identically if either property is deleted.
 */
record RememberMeNames(String parameterName, String cookieName) {

    /**
     * Spring Security's own default for both the remember-me request
     * parameter ({@code AbstractRememberMeServices.DEFAULT_PARAMETER}) and
     * the cookie name ({@code AbstractRememberMeServices.DEFAULT_COOKIE_NAME}) -
     * the two happen to share this literal in Spring Security itself, which
     * is exactly why Faz 3 originally used one constant for both. Faz 9
     * gives them independent configuration, but both still fall back to this
     * same Spring-provided default when unconfigured.
     */
    static final String SPRING_DEFAULT = "remember-me";

    static RememberMeNames from(String parameterNameProperty, String cookieNameProperty) {
        return new RememberMeNames(
                orSpringDefault(parameterNameProperty),
                orSpringDefault(cookieNameProperty));
    }

    private static String orSpringDefault(String configuredValue) {
        return (configuredValue == null || configuredValue.isBlank()) ? SPRING_DEFAULT : configuredValue;
    }

}
