package org.phoenix.multiplecustomauthproviders;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WhoAmIController {

    @GetMapping("/api/whoami")
    public String whoami(Authentication authentication) {
        return "authenticated as: " + authentication.getName();
    }
}
