package org.phoenix.rememberme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Faz 7 (UC-013 main scenario, A1, BR-019): with
 * {@code app.remember-me.strategy=persistent}, covers the Inspector's actual
 * job - listing username/series/token/last_used for existing records (main
 * scenario), the "no records yet" empty state (A1), and freshness after a
 * mutation (BR-019). See {@link TokenInspectorControllerTests} for A2
 * (token strategy active).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.remember-me.strategy=persistent")
class TokenInspectorPersistentModeTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void deleteRowsCreatedDuringTest() {
        jdbcTemplate.update("delete from persistent_logins where username = ?", DemoUserSeeder.DEMO_USERNAME);
    }

    private Cookie loginWithRememberMe() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login").with(csrf())
                        .param("username", DemoUserSeeder.DEMO_USERNAME)
                        .param("password", DemoUserSeeder.DEMO_PASSWORD)
                        .param("keep-me", "true"))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("notes-rm");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    @Test
    void a1NoRecordsYetReturnsPersistentStrategyWithAnEmptyList() throws Exception {
        mockMvc.perform(get("/api/token-inspector"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("PERSISTENT"))
                .andExpect(jsonPath("$.records").isEmpty());
    }

    @Test
    void listsUsernameSeriesTokenAndLastUsedForAnExistingRecord() throws Exception {
        Cookie cookie = loginWithRememberMe();
        String[] parts = PersistentCookieCodec.seriesAndToken(cookie);

        mockMvc.perform(get("/api/token-inspector"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("PERSISTENT"))
                .andExpect(jsonPath("$.records[0].username").value(DemoUserSeeder.DEMO_USERNAME))
                .andExpect(jsonPath("$.records[0].series").value(parts[0]))
                .andExpect(jsonPath("$.records[0].token").value(parts[1]))
                .andExpect(jsonPath("$.records[0].lastUsed").exists());
    }

    @Test
    void br019_reflectsTokenRotationOnTheVeryNextCallWithNoStaleCaching() throws Exception {
        Cookie originalCookie = loginWithRememberMe();
        String originalToken = PersistentCookieCodec.seriesAndToken(originalCookie)[1];

        mockMvc.perform(get("/api/token-inspector"))
                .andExpect(jsonPath("$.records[0].token").value(originalToken));

        // Auto-login rotates the token (BR-015) - no session attached, only
        // the remember-me cookie, same trigger as TokenRotationTests.
        mockMvc.perform(get("/api/me").cookie(originalCookie)).andExpect(status().isOk());

        // BR-019: immediately re-querying the Inspector - no wait, no
        // artificial delay - already reflects the new token, not the
        // pre-rotation one. There is no cache here to need 2 seconds to
        // expire; the freshness bound holds by construction.
        mockMvc.perform(get("/api/token-inspector"))
                .andExpect(jsonPath("$.records[0].token").value(Matchers.not(originalToken)));
    }

}
