package org.phoenix.rememberme;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal skeleton endpoint for Faz 0. Lets the frontend verify that it can
 * reach the backend. No auth logic here yet - that arrives in Faz 1.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "app", "remember-me-backend");
    }

}
