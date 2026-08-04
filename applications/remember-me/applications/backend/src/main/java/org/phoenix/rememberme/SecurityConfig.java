package org.phoenix.rememberme;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

/**
 * Faz 1: session-based form login (UC-001). Faz 3 adds token-based
 * remember-me (UC-002) and an explicit logout endpoint (UC-003).
 *
 * <p>The React SPA posts credentials to {@code /api/login} as a regular
 * {@code x-www-form-urlencoded} body, which Spring Security's built-in
 * {@code UsernamePasswordAuthenticationFilter} consumes directly - no
 * custom filter needed. Success/failure handlers reply with plain JSON
 * instead of the default redirects, since the frontend is an API client,
 * not a page Spring Security renders.
 *
 * <p>Unauthenticated access to any other endpoint gets a bare 401 (not a
 * redirect to a login page, and not an HTTP Basic challenge - the latter
 * makes browsers pop up their native auth dialog and hang the calling
 * {@code fetch()}, as found during Faz 0).
 *
 * <p>CSRF protection is out of scope for this lab (no requirement in
 * requirements.md/plan.md covers it across any phase); it is disabled here
 * rather than half-implemented.
 *
 * <p><b>Remember-me (UC-002, FR-002/FR-003):</b> {@code rememberMe()} wires
 * Spring Security's default {@code TokenBasedRememberMeServices} - a
 * signed, stateless cookie, not the database-backed persistent-token
 * variant (that swap is Faz 6's job). Nothing here hardcodes the
 * "token-based" choice beyond this one DSL call, so Faz 6 can later switch
 * to {@code .tokenRepository(...)} (persistent tokens) without touching
 * the login form, the logout config, or the cookie names below.
 *
 * <p>The remember-me parameter/cookie name stays Spring's default,
 * {@value #REMEMBER_ME_PARAMETER} - Faz 9 is what renames these, not this
 * phase. {@code TokenBasedRememberMeServices} hardcodes
 * {@code Cookie.setHttpOnly(true)} on the cookie it issues (verified
 * against the library bytecode, not assumed), so BR-004/NFR-001 holds
 * without any extra configuration here.
 *
 * <p><b>Auto-login and expiry (Faz 4, UC-004/UC-005):</b> Spring Security's
 * remember-me filter already performs auto-login on session loss the moment
 * {@code rememberMe()} is wired up (nothing new to build there); the only
 * gap this phase closes is making how long a token stays valid a config
 * value - {@code app.remember-me.token-validity-seconds} - instead of
 * Spring's hardcoded 14-day default, so it can be turned down for a fast
 * demo of BR-007 (expired cookie rejection). The resulting auto-login
 * produces a {@code RememberMeAuthenticationToken}, distinct from the
 * {@code UsernamePasswordAuthenticationToken} a real form login produces -
 * that distinction (BR-006) is what Faz 5's Anonymous/Remembered/Fully
 * Authenticated indicator is built on, though building that indicator
 * itself is Faz 5's job, not this one's.
 *
 * <p><b>Logout (UC-003, NFR-001):</b> {@code logout()} invalidates the
 * HTTP session, clears the remember-me authentication/tokens, and deletes
 * both cookies by name. {@code LogoutFilter} runs earlier in the filter
 * chain than the authorization check, so {@code POST /api/logout} still
 * runs every logout handler - including cookie clearing - even when the
 * session is already invalid server-side (A1): nothing here depends on the
 * caller being authenticated first.
 */
@Configuration
public class SecurityConfig {

    /**
     * Spring Security's default remember-me request parameter name, which
     * doubles as the cookie name issued by {@code TokenBasedRememberMeServices}
     * when no explicit cookie name is set. Named as a constant (rather than
     * left as an implicit default) so the login form, the security config,
     * and the logout cookie-clearing list can't silently drift apart -
     * Faz 9 is expected to replace this constant, not scatter the literal.
     */
    static final String REMEMBER_ME_PARAMETER = "remember-me";

    @Bean
    PasswordEncoder passwordEncoder() {
        // Strength 12 comfortably clears the NFR-002 floor of >= 10.
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
            @Value("${app.remember-me.key}") String rememberMeKey,
            @Value("${app.remember-me.token-validity-seconds}") int rememberMeTokenValiditySeconds) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health", "/api/logout").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginProcessingUrl("/api/login")
                        .successHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setCharacterEncoding("UTF-8");
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"username\":\"" + authentication.getName() + "\"}");
                        })
                        .failureHandler((request, response, exception) -> {
                            // BR-002: one generic message, regardless of whether the
                            // username or the password was wrong.
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setCharacterEncoding("UTF-8");
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"error\":\"Kullanıcı adı veya şifre hatalı.\"}");
                        })
                        .permitAll())
                .rememberMe(remember -> remember
                        // BR-003: no `alwaysRemember` - a cookie is only ever issued
                        // when the request actually carries this parameter with a
                        // truthy value, i.e. the checkbox was checked.
                        .key(rememberMeKey)
                        .rememberMeParameter(REMEMBER_ME_PARAMETER)
                        // Faz 4 (UC-005/BR-007): configurable instead of Spring's
                        // hardcoded 14-day default, so validity can be turned down
                        // (e.g. to 30s) to actually observe an expired cookie being
                        // rejected instead of waiting two weeks for it to happen.
                        .tokenValiditySeconds(rememberMeTokenValiditySeconds))
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        // JSON API, not a server-rendered app: reply with a bare
                        // status like the login handlers above do, and let the SPA
                        // decide what "back to the login page" means client-side.
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
                        .deleteCookies("JSESSIONID", REMEMBER_ME_PARAMETER)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

}
