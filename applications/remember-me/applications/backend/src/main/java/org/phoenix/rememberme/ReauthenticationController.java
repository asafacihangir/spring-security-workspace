package org.phoenix.rememberme;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC-009: upgrades a Remembered session to Fully Authenticated in place
 * (main scenario steps 2-4; A1 wrong password).
 *
 * <p>This endpoint sits behind {@code anyRequest().authenticated()} in
 * {@link SecurityConfig} - no extra rule needed, since a
 * {@code RememberMeAuthenticationToken}'s {@code isAuthenticated()} is
 * already true. An anonymous caller never reaches this method body at all
 * (rejected earlier in the filter chain with 401).
 *
 * <p><b>BR-012 (same account only):</b> the username checked is always
 * {@code authentication.getName()} - the CURRENTLY authenticated principal
 * carried by the Remembered session/cookie - never a value from the request
 * body. {@link ReauthenticateRequest} has no username field at all, so
 * there is nothing here for a caller to spoof into checking a different
 * account's password.
 *
 * <p><b>How the in-place upgrade actually works</b> (the architecturally
 * tricky part of this phase): on a correct password, this does not create a
 * new login or a new session through Spring Security's normal
 * authentication machinery (e.g. re-running {@code AuthenticationManager}
 * via a simulated form submit). Instead it replicates, by hand, exactly what
 * {@code AbstractAuthenticationProcessingFilter.successfulAuthentication(...)}
 * does (that is what form login runs through) between deciding "this
 * credential check succeeded" and replying to the caller:
 * <ol>
 * <li>builds a fresh {@code UsernamePasswordAuthenticationToken} via the
 * 3-arg constructor (principal, credentials, authorities) - that specific
 * constructor is documented to mark the token as already authenticated,
 * unlike the 2-arg one used for a not-yet-authenticated login attempt;
 * <li>installs it into a new {@link SecurityContext} via
 * {@code SecurityContextHolder.setContext(...)}, replacing whatever
 * {@code RememberMeAuthenticationToken} was there;
 * <li>calls {@link SessionAuthenticationStrategy#onAuthentication} on the
 * SAME strategy bean {@code SecurityConfig} wires into the filter chain's
 * {@code sessionManagement()} DSL - see below for why;
 * <li>explicitly calls {@link SecurityContextRepository#saveContext} on the
 * SAME repository bean {@code SecurityConfig} wires into the filter chain's
 * {@code securityContext()} DSL.
 * </ol>
 * That explicit save in step 4 is required because Spring Security 6's
 * {@code SecurityContextHolderFilter} only *loads* the context from the
 * repository at the start of a request - unlike the older, deprecated
 * {@code SecurityContextPersistenceFilter}, it does not auto-save whatever
 * ends up in {@code SecurityContextHolder} once the request completes. A
 * bare {@code SecurityContextHolder.setContext(...)} with no explicit save
 * would upgrade the level for the remainder of *this* request only and
 * silently revert to Remembered on the very next one - exactly the kind of
 * bug BR-009 (indicator always accurate) is designed to catch. Persisting
 * through {@code HttpSessionSecurityContextRepository} writes into the
 * caller's existing HTTP session if one is already present, or establishes
 * one if the caller had none yet (the common case for a Remembered caller,
 * since reaching "Remembered" in the first place - UC-004 - means the
 * session was already lost); either way, this is the SAME
 * {@code SecurityContext} object for the request/session Spring Security
 * already associated with this caller, not a parallel, second identity.
 *
 * <p>Step 3's session-id rotation is separately required to prevent session
 * fixation (CWE-384): without it, if a caller reaches this endpoint with an
 * ALREADY-LIVE {@code JSESSIONID} (not the typical Remembered case, which
 * has none, but a real possibility - e.g. a caller who still has a session
 * from before their remember-me cookie took over), that same session id
 * would carry across the privilege boundary unchanged. An attacker who
 * planted that id on the victim beforehand (classic fixation setup) would
 * then inherit the resulting fully-authenticated session. Calling the same
 * {@code SessionAuthenticationStrategy} the filter chain itself uses -
 * {@code ChangeSessionIdAuthenticationStrategy} by default, see
 * {@code SecurityConfig#sessionAuthenticationStrategy()} - rotates the id
 * via {@code HttpServletRequest#changeSessionId()} whenever a session
 * already exists, and is a documented no-op when it doesn't (nothing to
 * rotate away from yet), which the {@code saveContext} call immediately
 * afterward then turns into a brand-new, previously-unknown id anyway.
 */
@RestController
public class ReauthenticationController {

    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    public ReauthenticationController(UserRepository userRepository, UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder, SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy) {
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    @PostMapping("/api/reauthenticate")
    public ResponseEntity<Map<String, String>> reauthenticate(@RequestBody ReauthenticateRequest request,
            Authentication authentication, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        // A1: one generic-shaped failure whether the account somehow vanished
        // (shouldn't happen for an already-authenticated principal, but never
        // trust that) or the password just didn't match - no signal either
        // way about which, echoing BR-002's spirit from Faz 1 even though no
        // BR here explicitly demands it.
        if (user == null || request.password() == null
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Şifre doğrulanamadı."));
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        Authentication upgraded = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(upgraded);
        SecurityContextHolder.setContext(context);
        // CWE-384: rotate the session id across this privilege change before
        // persisting, exactly where AbstractAuthenticationProcessingFilter
        // calls the same strategy for a real form login - see class javadoc.
        sessionAuthenticationStrategy.onAuthentication(upgraded, httpRequest, httpResponse);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return ResponseEntity.ok(Map.of("level", AuthLevel.FULLY_AUTHENTICATED.name()));
    }

}
