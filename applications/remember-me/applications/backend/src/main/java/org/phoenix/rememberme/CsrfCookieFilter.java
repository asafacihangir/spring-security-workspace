package org.phoenix.rememberme;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Final-review fix-round (CSRF): forces the {@code XSRF-TOKEN} cookie to
 * actually be written on every request, which
 * {@code CookieCsrfTokenRepository} otherwise would not do for this app.
 *
 * <p><b>Why this filter has to exist:</b> since Spring Security 5.8,
 * {@code CsrfFilter} resolves the current request's {@link CsrfToken}
 * <em>lazily</em> - it hands the request a {@code Supplier<CsrfToken>}
 * wrapped by whatever {@code CsrfTokenRequestHandler} is configured (see
 * {@link SecurityConfig#filterChain}), but never itself calls
 * {@code .get()}/{@code .getToken()} on it. {@code CookieCsrfTokenRepository}
 * only writes its {@code Set-Cookie} header at the moment that deferred
 * supplier is actually resolved. In a server-rendered app, some view
 * template reading {@code ${_csrf.token}} (or similar) does that resolution
 * as a side effect of rendering the page. This app has no such template -
 * it is a pure JSON API - so without this filter, a plain {@code GET}
 * request (e.g. the SPA's initial {@code /api/auth-status} call) would
 * never cause the cookie to be issued at all, and the frontend would have
 * nothing to read for the {@code X-XSRF-TOKEN} header on its first mutating
 * request. This is Spring Security's own documented gotcha for exactly this
 * integration style (reference docs, "CSRF And Single Page Applications"),
 * not a workaround invented for this app.
 *
 * <p><b>The fix:</b> registered via {@code .addFilterAfter(this,
 * BasicAuthenticationFilter.class)} (Spring's own recommended position -
 * anywhere after {@code CsrfFilter} in the chain works, since all this does
 * is force resolution of the token {@code CsrfFilter} already attached to
 * the request as an attribute). Calling {@code csrfToken.getToken()} forces
 * the deferred supplier to run, which is what actually triggers
 * {@code CookieCsrfTokenRepository.saveToken(...)} and the resulting
 * {@code Set-Cookie: XSRF-TOKEN=...} header - after that, every subsequent
 * request from the same browser carries the cookie already, and this filter
 * is a no-op for it (the repository does not rewrite an unchanged cookie).
 */
final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Renders/resolves the deferred token, which is what actually
            // causes CookieCsrfTokenRepository to write the cookie - see
            // class javadoc.
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }

}
