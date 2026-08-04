package org.phoenix.rememberme;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Faz 1 has exactly one demo account. Seeds it on startup if it doesn't
 * already exist, hashing the password with the app's {@link PasswordEncoder}
 * bean (BCrypt, strength >= 10 - NFR-002) rather than hardcoding a
 * precomputed hash, so the plaintext never has to be typed anywhere except
 * here at hashing time.
 */
@Component
public class DemoUserSeeder implements CommandLineRunner {

    static final String DEMO_USERNAME = "demo";
    static final String DEMO_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername(DEMO_USERNAME).isEmpty()) {
            userRepository.save(new User(DEMO_USERNAME, passwordEncoder.encode(DEMO_PASSWORD)));
        }
    }

}
