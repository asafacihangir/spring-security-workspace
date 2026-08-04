package org.phoenix.rememberme;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;

/**
 * Faz 7 fix-round (security review finding): pure unit test locking in
 * {@link CookieTheftExceptionTranslationFilter}'s own mechanism, isolated
 * from the full Spring context and - crucially - from whatever access rule
 * {@code /error} happens to have at any given moment. Before this filter
 * existed, the app's clean {@code 401} on a theft replay only worked
 * because {@code /error} was not on the {@code permitAll} list (see that
 * class's javadoc, and task-7-report.md's fix-round entry, for the full
 * investigation); this filter's entire purpose is to make that outcome
 * independent of {@code /error}'s access rule. This class proves the
 * mechanism directly - no MySQL, no embedded server, no dependency on any
 * other part of {@link SecurityConfig} - so a regression here (someone
 * removing the try/catch, wiring the wrong entry point, or the delegate
 * chain no longer being invoked) fails fast and unambiguously, rather than
 * only being caught indirectly by {@link StolenCookieDetectionTests}
 * continuing to pass for what might turn out to be an unrelated reason
 * (exactly the failure mode the security review flagged in the first
 * place).
 */
class CookieTheftExceptionTranslationFilterTests {

    @Test
    void catchesCookieTheftExceptionAndDelegatesToTheAuthenticationEntryPoint() throws Exception {
        AuthenticationEntryPoint entryPoint = mock(AuthenticationEntryPoint.class);
        CookieTheftExceptionTranslationFilter filter = new CookieTheftExceptionTranslationFilter(entryPoint);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        CookieTheftException theft = new CookieTheftException("stolen");
        doThrow(theft).when(chain).doFilter(request, response);

        // Must not rethrow - letting it escape is exactly the bug this
        // filter exists to prevent.
        filter.doFilter(request, response, chain);

        verify(entryPoint, times(1)).commence(request, response, theft);
    }

    @Test
    void leavesAnOrdinaryRequestCompletelyUntouched() throws Exception {
        AuthenticationEntryPoint entryPoint = mock(AuthenticationEntryPoint.class);
        CookieTheftExceptionTranslationFilter filter = new CookieTheftExceptionTranslationFilter(entryPoint);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        verifyNoInteractions(entryPoint);
    }

}
