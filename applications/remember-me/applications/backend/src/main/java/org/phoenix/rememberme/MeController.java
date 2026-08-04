package org.phoenix.rememberme;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal session-protected endpoint. It stands in for the "protected page"
 * that Faz 1 needs to prove the post-login redirect works (the real notes
 * page is Faz 2's job - see task-1-brief.md).
 */
@RestController
public class MeController {

    @GetMapping("/api/me")
    public Map<String, String> me(Authentication authentication) {
        return Map.of("username", authentication.getName());
    }

}
