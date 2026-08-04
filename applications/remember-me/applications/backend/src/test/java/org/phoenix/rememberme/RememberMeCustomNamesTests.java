package org.phoenix.rememberme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-015 (Faz 9, FR-015, BR-022): automated coverage for Test Adimi 4 -
 * "Set-Cookie başlığında özel ad; özel paramla remember-me tetikleniyor,
 * varsayılan adla tetiklenmiyor" - plus the {@code GET /api/auth-status}
 * contract {@code LoginForm.jsx} relies on to learn the parameter name at
 * runtime without hardcoding it (see {@link AuthStatusController} and
 * {@link RememberMeNames}).
 *
 * <p>Runs against the real MySQL instance (task infra:up), same as
 * {@link FormLoginTests}, against this app's actual configured values
 * ({@code app.remember-me.parameter-name=keep-me},
 * {@code app.remember-me.cookie-name=notes-rm} in
 * {@code application.properties}). UC-015 A1 (property absent/blank ->
 * fallback to Spring's own default) is unit-tested in isolation instead by
 * {@link RememberMeNamesTests}, since that behavior needs no Spring context
 * at all and asserting it here would require standing up a second, oddly
 * configured application context just for one test.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RememberMeCustomNamesTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void setCookieHeaderCarriesTheConfiguredCustomCookieName() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login")
                        .param("username", DemoUserSeeder.DEMO_USERNAME)
                        .param("password", DemoUserSeeder.DEMO_PASSWORD)
                        .param("keep-me", "true"))
                .andExpect(status().isOk())
                .andReturn();

        // Raw header check (what Test Adimi 1's DevTools inspection is
        // actually looking at), not just MockMvc's typed Cookie API below -
        // proves the literal Set-Cookie header name is "notes-rm", not
        // Spring's "remember-me" default.
        Collection<String> setCookieHeaders = result.getResponse().getHeaders("Set-Cookie");
        assertThat(setCookieHeaders)
                .as("Set-Cookie must carry the configured cookie name, not Spring's 'remember-me' default")
                .anyMatch(header -> header.startsWith("notes-rm="));

        Cookie cookie = result.getResponse().getCookie("notes-rm");
        assertThat(cookie).isNotNull();
    }

    @Test
    void customParameterNameTriggersRememberMe() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login")
                        .param("username", DemoUserSeeder.DEMO_USERNAME)
                        .param("password", DemoUserSeeder.DEMO_PASSWORD)
                        .param("keep-me", "true"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getCookie("notes-rm"))
                .as("the configured parameter name must trigger a remember-me cookie")
                .isNotNull();
    }

    @Test
    void a2DefaultSpringParameterNameDoesNotTriggerRememberMeButLoginStillSucceeds() throws Exception {
        // UC-015 A2: the request carries a parameter name the backend isn't
        // listening for (Spring Security's own default, "remember-me",
        // instead of this app's configured "keep-me"). Spring Security's
        // RememberMeAuthenticationFilter/AbstractRememberMeServices only
        // ever look for the exact configured parameter name - anything else
        // present in the request body is simply not read, not treated as an
        // error - so this proves login succeeds regardless (username/password
        // were still correct) and simply produces no remember-me cookie,
        // rather than assuming that behavior.
        MvcResult result = mockMvc.perform(post("/api/login")
                        .param("username", DemoUserSeeder.DEMO_USERNAME)
                        .param("password", DemoUserSeeder.DEMO_PASSWORD)
                        .param("remember-me", "true"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getCookie("notes-rm"))
                .as("a mismatched parameter name must not trigger the configured remember-me cookie")
                .isNull();
        // Also confirms Spring's own default cookie name isn't produced
        // either - the mismatched request truly triggers nothing.
        assertThat(result.getResponse().getCookie("remember-me")).isNull();
    }

    @Test
    void authStatusExposesTheConfiguredParameterNameForTheFrontendToUse() throws Exception {
        // The actual single-source-of-truth mechanism LoginForm.jsx relies
        // on (BR-022): the frontend never hardcodes "keep-me" as a JS string
        // literal - it reads this response instead, so
        // application.properties (via the shared RememberMeNames bean) is
        // the one place both sides agree on the name.
        mockMvc.perform(get("/api/auth-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rememberMeParameter").value("keep-me"));
    }

}
