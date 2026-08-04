package org.phoenix.rememberme;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Faz 7 (UC-013 A2): with the default {@code app.remember-me.strategy=token}
 * (no {@code @TestPropertySource} override - this class shares that
 * property with most of the suite, same convention as
 * {@link RememberMeStrategyDoesNotWriteToPersistentLoginsTests}), proves
 * the Inspector endpoint tells the caller "wrong mode" instead of a bare
 * empty list - and that it needs no authentication at all to do so.
 *
 * <p>See {@link TokenInspectorPersistentModeTests} for A1 and the happy
 * path (persistent mode with an actual record).
 */
@SpringBootTest
@AutoConfigureMockMvc
class TokenInspectorControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void a2TokenStrategyActiveReportsStrategyInsteadOfABareEmptyList() throws Exception {
        mockMvc.perform(get("/api/token-inspector"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("TOKEN"))
                .andExpect(jsonPath("$.records").isEmpty());
    }

    @Test
    void endpointRequiresNoAuthentication() throws Exception {
        // No session, no cookies at all - a debugging tool, not a
        // user-scoped resource. See TokenInspectorController's javadoc.
        mockMvc.perform(get("/api/token-inspector"))
                .andExpect(status().isOk());
    }

}
