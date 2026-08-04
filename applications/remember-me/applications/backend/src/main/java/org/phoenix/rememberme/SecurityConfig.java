package org.phoenix.rememberme;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * <b>What this filter chain does today</b> (all 11 phases, final-review
 * fix-round): session-based form login at {@code /api/login} for a single
 * demo user; opt-in remember-me that is either a stateless HMAC cookie or a
 * database-backed {@code persistent_logins} record, switched purely by
 * {@code app.remember-me.strategy} and optionally bound to the client IP
 * ({@code app.remember-me.ip-binding-enabled}, persistent-only); logout at
 * {@code /api/logout} that clears both the session and every remember-me
 * cookie/record; {@code isFullyAuthenticated()}-gated {@code /api/account}
 * so a remembered-only session cannot touch account settings without
 * re-entering the password; CSRF protection via a JS-readable
 * {@code XSRF-TOKEN} cookie and an {@code X-XSRF-TOKEN} request header,
 * covering every mutating endpoint; and two intentionally {@code permitAll}
 * teaching endpoints, {@code /api/auth-status} and
 * {@code /api/token-inspector} (the latter exposes live remember-me
 * credentials with no auth gate on purpose - see that controller's own
 * javadoc for why, and the explicit warning attached to it). The
 * phase-by-phase narrative below records how each piece arrived and why it
 * looks the way it does; it is kept as history, not as the primary
 * reference for "what does this class do right now."
 *
 * <p>Faz 1: session-based form login (UC-001). Faz 3 adds token-based
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
 * <p><b>CSRF (final-review fix-round):</b> enabled via
 * {@code CookieCsrfTokenRepository.withHttpOnlyFalse()} - the token cookie
 * ({@code XSRF-TOKEN}) must be {@code HttpOnly=false} so the SPA's own
 * JavaScript can read it and echo it back as the {@code X-XSRF-TOKEN}
 * header on every mutating request (POST/PUT/DELETE); see {@code api.js} on
 * the frontend for that half. This does not weaken anything: unlike
 * {@code JSESSIONID} and the remember-me cookie (both still {@code HttpOnly},
 * NFR-001 untouched), the CSRF token cookie is not a credential by itself -
 * an attacker who can read it via XSS could forge a matching request anyway,
 * with or without CSRF protection in place, and this token proves nothing
 * except "some script running on this origin saw the cookie", which is
 * exactly what the header round-trip is designed to require from a
 * same-origin script and deny to a cross-origin form/link. The handler is
 * {@link CsrfTokenRequestAttributeHandler} rather than Spring Security's
 * BREACH-protection-aware default ({@code XorCsrfTokenRequestAttributeHandler}):
 * that default defers token resolution and masks the value it writes to the
 * response, which is the right choice for server-rendered pages but is
 * incompatible with a plain SPA pattern where JS reads the raw cookie value
 * directly and sends it back unmodified - Spring's own reference docs
 * recommend this exact swap for that integration style. One more piece is
 * needed beyond the DSL config here: see {@link CsrfCookieFilter}, added
 * near the bottom of this method via {@code addFilterAfter(...)}, for why
 * the cookie would otherwise never actually get written for a pure JSON
 * API.
 *
 * <p><b>Remember-me (UC-002, FR-002/FR-003):</b> {@code rememberMe()} wires
 * either Spring Security's default {@code TokenBasedRememberMeServices} - a
 * signed, stateless cookie - or the database-backed
 * {@code PersistentTokenBasedRememberMeServices} variant, chosen at startup
 * by {@code app.remember-me.strategy} (Faz 6, UC-010, BR-013/BR-014,
 * NFR-005; see {@link RememberMeStrategy} and the {@code rememberMe(...)}
 * customizer in {@link #filterChain}). Nothing else in this class - the
 * login form, the logout config, the cookie names below - differs between
 * the two strategies; the property is the only thing that changes.
 *
 * <p><b>Custom names (Faz 9, UC-015, BR-022):</b> the remember-me request
 * parameter name and cookie name are no longer the hardcoded Spring default -
 * both are resolved once by the {@link #rememberMeNames} bean from
 * {@code app.remember-me.parameter-name}/{@code app.remember-me.cookie-name}
 * (this app's chosen values, {@code keep-me}/{@code notes-rm}), with a
 * graceful fallback to Spring's own default when either property is absent
 * or blank (UC-015 A1). See {@link RememberMeNames}'s javadoc for why this
 * is one bean rather than two independently-read properties, and for how
 * the frontend learns the parameter name without hardcoding it.
 * {@code TokenBasedRememberMeServices} hardcodes {@code Cookie.setHttpOnly(true)}
 * on the cookie it issues (verified against the library bytecode, not
 * assumed), so BR-004/NFR-001 holds without any extra configuration here.
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
 *
 * <p><b>Faz 5 (UC-007/008/009):</b> two more pieces live here beyond the
 * plain {@code authorizeHttpRequests} rules:
 * <ul>
 * <li>{@code /api/auth-status} is {@code permitAll} - it is UC-007's
 * indicator source and must answer "Anonymous" for an anonymous caller
 * too, not 401 it away.
 * <li>{@code /api/account} (and any sub-path) uses
 * {@code .access(new WebExpressionAuthorizationManager("isFullyAuthenticated()"))}
 * instead of the plain {@code authenticated()} the rest of the app uses.
 * This is BR-010/BR-011's actual enforcement point: {@code authenticated()}
 * alone would let a {@code RememberMeAuthenticationToken} through (its
 * {@code isAuthenticated()} is true), which is exactly the gap a Remembered
 * session viewing/changing account info would exploit. The
 * {@code isFullyAuthenticated()} SpEL rule, backed by Spring Security's
 * {@code AuthenticatedVoter}/{@code AuthenticationTrustResolver} semantics,
 * is what actually excludes remember-me-only sessions - see
 * {@link AuthLevel} for the same trust-resolver logic reused for the
 * indicator itself.
 * </ul>
 * The {@link #securityContextRepository()} and
 * {@link #sessionAuthenticationStrategy()} beans below are both shared with
 * {@link ReauthenticationController}, which needs the exact same instances
 * to explicitly persist an upgraded {@code Authentication} back into the
 * caller's session and to rotate the session id across that privilege
 * change (see that class's javadoc for why both calls have to be explicit
 * in Spring Security 6 when the upgrade isn't done through a
 * {@code AbstractAuthenticationProcessingFilter} subclass).
 *
 * <p><b>Faz 10 (UC-016/UC-017, FR-016/FR-017): IP-bound persistent
 * remember-me.</b> When {@code app.remember-me.strategy=persistent}, the
 * {@code .rememberMe(...)} DSL below no longer calls {@code .tokenRepository(...)}
 * to let {@code RememberMeConfigurer} build a stock
 * {@code PersistentTokenBasedRememberMeServices} itself; instead it
 * constructs {@link IpBoundPersistentTokenBasedRememberMeServices} directly
 * and hands it to {@code .rememberMeServices(...)}. This is required, not
 * a style choice: {@code RememberMeConfigurer} has no hook to substitute a
 * custom subclass via {@code .tokenRepository(...)} (verified against its
 * source - {@code createPersistentRememberMeServices} always {@code new}s
 * the stock class), and its {@code validateInput()} throws
 * ("Can not set rememberMeCookieName and custom rememberMeServices") if
 * both {@code .rememberMeCookieName(...)} and {@code .rememberMeServices(...)}
 * are called on the same configurer - which this app's custom cookie name
 * (Faz 9, UC-015) would otherwise do. So the PERSISTENT branch below never
 * calls {@code .rememberMeCookieName(...)}/{@code .rememberMeParameter(...)}/
 * {@code .tokenValiditySeconds(...)} on the DSL at all; it applies the exact
 * same three settings directly on the constructed instance
 * ({@code setCookieName}/{@code setParameter}/{@code setTokenValiditySeconds}),
 * mirroring exactly what {@code RememberMeConfigurer#getRememberMeServices}
 * would otherwise have done for a DSL-created instance. The TOKEN branch is
 * completely unchanged from Faz 3/6/9 - {@link IpBoundPersistentTokenBasedRememberMeServices}
 * is never constructed, referenced, or relevant when that strategy is
 * active (the Checkpoint 10 regression check). See that class's own javadoc
 * for the IP-binding logic itself and the "persistent-only" scope decision.
 */
@Configuration
public class SecurityConfig {

    /**
     * Faz 9 (UC-015, BR-022): the one bean that resolves both remember-me
     * names from configuration - see {@link RememberMeNames}'s javadoc for
     * why this indirection exists instead of reading the two properties
     * directly wherever they're needed. The {@code :} defaults below (empty
     * string) intentionally let {@link RememberMeNames#from} see "absent"
     * as blank rather than as a missing-property exception, so A1 degrades
     * gracefully instead of failing application startup.
     */
    @Bean
    RememberMeNames rememberMeNames(
            @Value("${app.remember-me.parameter-name:}") String parameterNameProperty,
            @Value("${app.remember-me.cookie-name:}") String cookieNameProperty) {
        return RememberMeNames.from(parameterNameProperty, cookieNameProperty);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // Strength 12 comfortably clears the NFR-002 floor of >= 10.
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Faz 5: {@link AuthLevel} and {@link AuthStatusController} classify the
     * current {@code Authentication} purely off what this resolver already
     * knows (Anonymous/Remembered) - a single Spring Security-provided
     * source of truth, not a second, hand-rolled notion of "level".
     */
    @Bean
    AuthenticationTrustResolver authenticationTrustResolver() {
        return new AuthenticationTrustResolverImpl();
    }

    /**
     * Faz 5: exposed as its own bean (rather than left as the DSL's
     * implicit default) so {@link ReauthenticationController} can inject
     * this exact instance and explicitly persist an upgraded
     * {@code Authentication} into it - see that class's javadoc for why the
     * save must be explicit under Spring Security 6's
     * {@code SecurityContextHolderFilter}. Plain
     * {@code HttpSessionSecurityContextRepository} (not the
     * request-attribute-wrapping {@code Delegating} variant the DSL would
     * otherwise default to) is enough here: this app is entirely
     * session-based already, nothing stateless to optimize around.
     */
    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Faz 5 fix-round: session-fixation protection (CWE-384) for
     * {@link ReauthenticationController}'s privilege upgrade. Spring
     * Security's own {@code AbstractAuthenticationProcessingFilter} (what
     * runs form login) always calls
     * {@code sessionAuthenticationStrategy.onAuthentication(...)} between
     * installing the new {@code Authentication} into the
     * {@code SecurityContext} and persisting it - that call is what rotates
     * the session id whenever the privilege level changes, so a session id
     * an attacker planted before the victim authenticated (classic fixation
     * setup) is never the same id the victim ends up fully authenticated
     * under. The controller-driven upgrade path bypasses that filter
     * entirely, so it has to invoke the exact same strategy itself -
     * exposed as a bean here (mirroring {@link #securityContextRepository()}
     * above) so both this filter chain and the controller share one
     * instance rather than each configuring their own.
     * {@code ChangeSessionIdAuthenticationStrategy} is Spring Security's own
     * default for this slot (Servlet 3.1+ {@code HttpServletRequest#changeSessionId()},
     * no new session/cookie round-trip the way session-invalidation-based
     * strategies would need) and is a no-op when the caller has no session
     * yet - exactly the common Remembered-caller case, where there is no
     * pre-existing id to rotate away from.
     */
    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    /**
     * Faz 6 (UC-010, FR-010): Spring Security's own JDBC-backed
     * {@link PersistentTokenRepository}, pointed at the same
     * {@link DataSource} {@code users}/{@code notes} already use. Wired
     * unconditionally - not behind {@code @ConditionalOnProperty} or a
     * profile - so the {@code persistent_logins} table/repository exist
     * identically whichever strategy {@link #filterChain} ends up selecting
     * (BR-013: the switch is a single property read inside one
     * {@code @Bean} method, not a different set of beans coming into
     * existence). {@code setCreateTableOnStartup} is deliberately left at
     * its default {@code false}: {@link PersistentLogin} (an
     * {@code @Entity}, unconditionally on the classpath) is what gives this
     * table its schema, via the same
     * {@code spring.jpa.hibernate.ddl-auto=update} mechanism
     * {@code users}/{@code notes} already rely on - see that class's
     * javadoc for the full reasoning.
     */
    @Bean
    PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(dataSource);
        return repository;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
            @Value("${app.remember-me.key}") String rememberMeKey,
            @Value("${app.remember-me.token-validity-seconds}") int rememberMeTokenValiditySeconds,
            @Value("${app.remember-me.strategy}") String rememberMeStrategy,
            @Value("${app.remember-me.ip-binding-enabled:false}") boolean ipBindingEnabled,
            RememberMeNames rememberMeNames,
            PersistentTokenRepository persistentTokenRepository,
            UserDetailsService userDetailsService,
            JdbcTemplate jdbcTemplate,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            ObjectMapper objectMapper) throws Exception {
        // Faz 7 fix-round: shared with both .exceptionHandling(...) below and
        // CookieTheftExceptionTranslationFilter, so a theft replay and every
        // other unauthenticated request produce byte-for-byte the same 401 -
        // see that filter's javadoc for why it exists at all.
        AuthenticationEntryPoint authenticationEntryPoint = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session
                        .sessionAuthenticationStrategy(sessionAuthenticationStrategy))
                .authorizeHttpRequests(auth -> auth
                        // Faz 7 (UC-013): /api/token-inspector joins /api/auth-status here -
                        // this exposes live remember-me bearer credentials with zero auth,
                        // solely because UC-012's stolen-cookie walkthrough requires copying a
                        // real cookie value; see TokenInspectorController's javadoc "Access
                        // control" section for the full reasoning and an explicit warning that
                        // no production system should ever expose an endpoint like this.
                        .requestMatchers("/api/health", "/api/logout", "/api/auth-status", "/api/token-inspector")
                        .permitAll()
                        // BR-010/BR-011: isFullyAuthenticated(), not just authenticated() -
                        // a RememberMeAuthenticationToken satisfies the latter but must not
                        // satisfy the former. See class javadoc.
                        .requestMatchers("/api/account", "/api/account/**")
                        .access(new WebExpressionAuthorizationManager("isFullyAuthenticated()"))
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginProcessingUrl("/api/login")
                        .successHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setCharacterEncoding("UTF-8");
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            // Map.of(...) + ObjectMapper, not string concatenation, so a
                            // username containing a quote character can never produce
                            // malformed JSON - consistent with how every other controller
                            // in this app (AuthStatusController, MeController, ...) builds
                            // its response body.
                            objectMapper.writeValue(response.getWriter(),
                                    Map.of("username", authentication.getName()));
                        })
                        .failureHandler((request, response, exception) -> {
                            // BR-002: one generic message, regardless of whether the
                            // username or the password was wrong.
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setCharacterEncoding("UTF-8");
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getWriter(),
                                    Map.of("error", "Kullanıcı adı veya şifre hatalı."));
                        })
                        .permitAll())
                .rememberMe(remember -> {
                    // Faz 6 (UC-010, BR-013/BR-014) / Faz 10 (UC-016/UC-017): the
                    // one branch that decides which RememberMeServices Spring
                    // Security actually ends up using. TOKEN keeps Faz 3/9's
                    // exact behavior (BR-003's no-alwaysRemember, Faz 9's custom
                    // names, Faz 4's configurable validity - all applied to the
                    // DSL as before). PERSISTENT can no longer go through
                    // .tokenRepository(...) once IP-binding needs a custom
                    // RememberMeServices subclass - see this class's javadoc
                    // "Faz 10" section and IpBoundPersistentTokenBasedRememberMeServices's
                    // javadoc for the full reasoning and mechanics.
                    if (RememberMeStrategy.from(rememberMeStrategy) == RememberMeStrategy.PERSISTENT) {
                        IpBoundPersistentTokenBasedRememberMeServices ipBoundServices =
                                new IpBoundPersistentTokenBasedRememberMeServices(
                                        rememberMeKey, userDetailsService, persistentTokenRepository,
                                        jdbcTemplate, ipBindingEnabled);
                        // Mirrors exactly what RememberMeConfigurer#getRememberMeServices
                        // would otherwise have applied to a DSL-created instance -
                        // see this class's javadoc for why these three calls have
                        // to happen here instead of on the `remember` DSL.
                        ipBoundServices.setParameter(rememberMeNames.parameterName());
                        ipBoundServices.setCookieName(rememberMeNames.cookieName());
                        ipBoundServices.setTokenValiditySeconds(rememberMeTokenValiditySeconds);
                        ipBoundServices.afterPropertiesSet();
                        remember.rememberMeServices(ipBoundServices);
                    } else {
                        remember
                                // BR-003: no `alwaysRemember` - a cookie is only ever
                                // issued when the request actually carries this
                                // parameter with a truthy value, i.e. the checkbox
                                // was checked.
                                .key(rememberMeKey)
                                // Faz 9 (UC-015, BR-022): both names come from the one
                                // RememberMeNames bean - see its javadoc and this
                                // class's javadoc "Custom names" section.
                                .rememberMeParameter(rememberMeNames.parameterName())
                                .rememberMeCookieName(rememberMeNames.cookieName())
                                // Faz 4 (UC-005/BR-007): configurable instead of
                                // Spring's hardcoded 14-day default, so validity can
                                // be turned down (e.g. to 30s) to actually observe an
                                // expired cookie being rejected instead of waiting two
                                // weeks for it to happen.
                                .tokenValiditySeconds(rememberMeTokenValiditySeconds);
                    }
                })
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        // JSON API, not a server-rendered app: reply with a bare
                        // status like the login handlers above do, and let the SPA
                        // decide what "back to the login page" means client-side.
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler())
                        // Faz 9 regression check: this must delete the actual COOKIE
                        // name (rememberMeNames.cookieName(), "notes-rm"), not the
                        // parameter name - the two are independently configured now
                        // and are no longer guaranteed to be the same string.
                        .deleteCookies("JSESSIONID", rememberMeNames.cookieName())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint))
                // Faz 7 fix-round (UC-012): explicit control for
                // CookieTheftException, which ExceptionTranslationFilter can
                // never see on its own - RememberMeAuthenticationFilter runs
                // before it in the default filter order, so an exception
                // thrown from inside autoLogin() escapes before
                // ExceptionTranslationFilter gets a turn. Positioned directly
                // before RememberMeAuthenticationFilter so that filter's call
                // happens inside THIS filter's own chain.doFilter(...) - see
                // CookieTheftExceptionTranslationFilter's javadoc for the
                // full mechanics and why this was needed at all.
                .addFilterBefore(new CookieTheftExceptionTranslationFilter(authenticationEntryPoint),
                        RememberMeAuthenticationFilter.class)
                // CSRF fix-round: forces the XSRF-TOKEN cookie to actually be
                // written on every request - see CsrfCookieFilter's javadoc for
                // why CookieCsrfTokenRepository alone does not do this for a
                // pure JSON API with no server-rendered view to trigger it.
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
        return http.build();
    }

}
