package org.phoenix.rememberme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Faz 6 (UC-010 A1): pure unit coverage for {@link RememberMeStrategy#from},
 * the one place an unrecognized {@code app.remember-me.strategy} value gets
 * turned into a startup-failing exception rather than a silent default. No
 * Spring context needed here - {@link RememberMeApplicationFailsFastOnInvalidStrategyTests}
 * separately proves the exception actually reaches application startup.
 */
class RememberMeStrategyTests {

    @Test
    void recognizesTokenCaseInsensitively() {
        assertThat(RememberMeStrategy.from("token")).isEqualTo(RememberMeStrategy.TOKEN);
        assertThat(RememberMeStrategy.from("TOKEN")).isEqualTo(RememberMeStrategy.TOKEN);
        assertThat(RememberMeStrategy.from("Token")).isEqualTo(RememberMeStrategy.TOKEN);
    }

    @Test
    void recognizesPersistentCaseInsensitively() {
        assertThat(RememberMeStrategy.from("persistent")).isEqualTo(RememberMeStrategy.PERSISTENT);
        assertThat(RememberMeStrategy.from("PERSISTENT")).isEqualTo(RememberMeStrategy.PERSISTENT);
    }

    @Test
    void a1UnrecognizedValueThrowsInsteadOfDefaulting() {
        assertThatThrownBy(() -> RememberMeStrategy.from("bogus"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bogus")
                .hasMessageContaining("app.remember-me.strategy");
    }

    @Test
    void a1EmptyValueThrows() {
        assertThatThrownBy(() -> RememberMeStrategy.from(""))
                .isInstanceOf(IllegalStateException.class);
    }

}
