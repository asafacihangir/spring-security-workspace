package org.phoenix.rememberme;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Faz 7 fix-round (UC-012, security review finding): makes theft
 * detection's clean-401 outcome an explicit control instead of an
 * incidental side effect of {@code /error}'s access rule.
 *
 * <p><b>Why this filter has to exist:</b>
 * {@code PersistentTokenBasedRememberMeServices.processAutoLoginCookie}
 * throws {@code CookieTheftException} on a series/token mismatch, and
 * {@code AbstractRememberMeServices.autoLogin} deliberately re-throws it -
 * the one exception type out of several it does <em>not</em> just log and
 * swallow - after already cancelling the offending cookie and evicting the
 * compromised record(s). {@code RememberMeAuthenticationFilter.doFilter}
 * calls {@code rememberMeServices.autoLogin(request, response)} with no
 * try/catch around that specific call (its own try/catch only wraps the
 * later {@code authenticationManager.authenticate(...)} call, and only for
 * a non-null {@code autoLogin()} result) - so the exception propagates
 * straight out of that filter uncaught.
 *
 * <p>Ordinarily {@code ExceptionTranslationFilter} is exactly what turns an
 * escaping {@code AuthenticationException} into this app's
 * {@code HttpStatusEntryPoint(UNAUTHORIZED)} response (see
 * {@link SecurityConfig#filterChain}'s {@code exceptionHandling(...)}). It
 * cannot do that for this specific exception: Spring Security's default
 * filter order ({@code FilterOrderRegistration}) places
 * {@code RememberMeAuthenticationFilter} <em>before</em>
 * {@code ExceptionTranslationFilter}, and a filter can only catch
 * exceptions thrown by filters that run <em>after</em> it, inside its own
 * {@code chain.doFilter(...)} call - never ones thrown by a filter that
 * already ran and handed control back before it got a turn. Verified
 * empirically (not just from the source): before this filter existed, a
 * theft replay's {@code CookieTheftException} showed no
 * {@code ExceptionTranslationFilter} frame anywhere in its stack trace,
 * under both {@code MockMvc} (where it then propagated all the way out of
 * the test, uncaught) and a real embedded server (where it happened to
 * still resolve to a clean {@code 401} only because Spring Boot's default
 * {@code /error} forward is itself not on this app's {@code permitAll}
 * list - see task-7-report.md for the full investigation).
 *
 * <p><b>The fix:</b> register this filter via {@code .addFilterBefore(this,
 * RememberMeAuthenticationFilter.class)}, which places it one slot earlier
 * in the chain than {@code RememberMeAuthenticationFilter}. That means
 * {@code RememberMeAuthenticationFilter}'s own {@code doFilter} call - and
 * the {@code autoLogin()} call inside it - happens as part of <em>this</em>
 * filter's own {@code chain.doFilter(...)} invocation, so a
 * {@code CookieTheftException} thrown from inside it unwinds back into this
 * filter's try/catch. This is the exact same structural trick
 * {@code ExceptionTranslationFilter} itself relies on to catch exceptions
 * from filters after it - just positioned one slot earlier, scoped to this
 * one exception type, so it actually gets a chance to see it. On catch, it
 * delegates to the same {@link AuthenticationEntryPoint} instance the rest
 * of {@code exceptionHandling()} uses, so a theft replay and every other
 * unauthenticated request in this app get byte-for-byte the same {@code 401}
 * response - not a bespoke one - and the outcome no longer depends on
 * whatever access rule {@code /error} happens to have.
 */
final class CookieTheftExceptionTranslationFilter extends OncePerRequestFilter {

    private final AuthenticationEntryPoint authenticationEntryPoint;

    CookieTheftExceptionTranslationFilter(AuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (CookieTheftException ex) {
            this.authenticationEntryPoint.commence(request, response, ex);
        }
    }

}
