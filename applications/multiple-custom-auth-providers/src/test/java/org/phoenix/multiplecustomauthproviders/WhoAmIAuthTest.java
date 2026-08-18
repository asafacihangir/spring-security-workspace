package org.phoenix.multiplecustomauthproviders;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WhoAmIAuthTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void basicAuth_valid_200() throws Exception {
        mockMvc.perform(get("/api/whoami").with(httpBasic("user", "password")))
                .andExpect(status().isOk());
    }

    @Test
    void apiKey_valid_200() throws Exception {
        mockMvc.perform(get("/api/whoami").header("X-API-KEY", "secret-api-key"))
                .andExpect(status().isOk());
    }

    @Test
    void noCredentials_401() throws Exception {
        mockMvc.perform(get("/api/whoami"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidApiKey_401() throws Exception {
        mockMvc.perform(get("/api/whoami").header("X-API-KEY", "wrong"))
                .andExpect(status().isUnauthorized());
    }
}
