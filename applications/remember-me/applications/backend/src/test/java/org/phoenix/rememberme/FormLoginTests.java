package org.phoenix.rememberme;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test Adımı 6: automated coverage for UC-001's main scenario, A1 (wrong
 * credentials -> generic error, BR-002), and the "protected endpoint
 * rejects unauthenticated requests" result.
 *
 * Runs against the real MySQL instance (task infra:up) like the rest of
 * this app's tests - see task-0-report.md - so the demo user seeded by
 * {@link DemoUserSeeder} on context startup is what these tests log in as.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FormLoginTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void correctCredentialsLogInSuccessfully() throws Exception {
        mockMvc.perform(formLogin("/api/login")
                        .user(DemoUserSeeder.DEMO_USERNAME)
                        .password(DemoUserSeeder.DEMO_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(DemoUserSeeder.DEMO_USERNAME)));
    }

    @Test
    void wrongPasswordGetsGenericError() throws Exception {
        mockMvc.perform(formLogin("/api/login")
                        .user(DemoUserSeeder.DEMO_USERNAME)
                        .password("not-the-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Kullanıcı adı veya şifre hatalı")));
    }

    @Test
    void unknownUsernameGetsTheSameGenericError() throws Exception {
        // BR-002: identical failure response whether the username or the
        // password was wrong - no field-specific hints.
        mockMvc.perform(formLogin("/api/login")
                        .user("no-such-user")
                        .password("whatever"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Kullanıcı adı veya şifre hatalı")));
    }

    @Test
    void protectedEndpointRejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

}
