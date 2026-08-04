package org.phoenix.rememberme;

import java.util.Map;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC-007: single always-available source of truth for the auth-level
 * indicator (BR-009). Permitted for every caller including anonymous ones -
 * see SecurityConfig's {@code permitAll} for this path - since an anonymous
 * learner needs to see "Anonymous" here too, not a 401.
 *
 * <p>Faz 9 (UC-015, BR-022): also carries {@code rememberMeParameter}, the
 * currently-configured remember-me request parameter name, resolved by the
 * same {@link RememberMeNames} bean {@link SecurityConfig#filterChain} uses
 * to configure Spring Security itself. This is how the frontend learns what
 * field name to send under - {@code LoginForm.jsx} reads this response
 * rather than hardcoding {@code "keep-me"} as a JS string literal - so
 * {@code application.properties} stays the one place that name is actually
 * decided, on both sides of the API boundary. The cookie name is
 * deliberately NOT included here; see {@link RememberMeNames}'s javadoc for
 * why the frontend never needs it.
 */
@RestController
public class AuthStatusController {

    private final AuthenticationTrustResolver trustResolver;
    private final RememberMeNames rememberMeNames;

    public AuthStatusController(AuthenticationTrustResolver trustResolver, RememberMeNames rememberMeNames) {
        this.trustResolver = trustResolver;
        this.rememberMeNames = rememberMeNames;
    }

    @GetMapping("/api/auth-status")
    public Map<String, String> authStatus(Authentication authentication) {
        return Map.of(
                "level", AuthLevel.of(authentication, trustResolver).name(),
                "rememberMeParameter", rememberMeNames.parameterName());
    }

}
