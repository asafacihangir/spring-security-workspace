package org.phoenix.rememberme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @EnableScheduling} (Faz 8, UC-014): turns on Spring's
 * {@code @Scheduled} support so {@link ExpiredPersistentLoginCleanupJob} runs
 * on its configured interval. No other phase needs a background job, so this
 * is the one place scheduling is switched on, rather than a dedicated config
 * class for a single annotation.
 */
@SpringBootApplication
@EnableScheduling
public class RememberMeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RememberMeApplication.class, args);
    }

}
