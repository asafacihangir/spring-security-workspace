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
 */
@RestController
public class AuthStatusController {

    private final AuthenticationTrustResolver trustResolver;

    public AuthStatusController(AuthenticationTrustResolver trustResolver) {
        this.trustResolver = trustResolver;
    }

    @GetMapping("/api/auth-status")
    public Map<String, String> authStatus(Authentication authentication) {
        return Map.of("level", AuthLevel.of(authentication, trustResolver).name());
    }

}
