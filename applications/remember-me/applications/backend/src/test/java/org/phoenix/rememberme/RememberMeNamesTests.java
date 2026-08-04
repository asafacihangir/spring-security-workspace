package org.phoenix.rememberme;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * UC-015 A1: plain unit tests (no Spring context needed) proving
 * {@link RememberMeNames#from} degrades gracefully to Spring Security's own
 * default whenever a configured name is absent or blank, rather than
 * throwing or producing an unusable empty name. The "configured" happy path
 * is covered end to end by {@link RememberMeCustomNamesTests} instead, since
 * that needs the full Spring Security filter chain wired up to be
 * meaningful.
 */
class RememberMeNamesTests {

    @Test
    void bothConfiguredValuesAreUsedAsIs() {
        RememberMeNames names = RememberMeNames.from("keep-me", "notes-rm");

        assertThat(names.parameterName()).isEqualTo("keep-me");
        assertThat(names.cookieName()).isEqualTo("notes-rm");
    }

    @Test
    void bothMissingFallBackToSpringsOwnDefaultForBoth() {
        // Spring's @Value("${...:}") default (empty string) is what an
        // absent property actually resolves to - not null - so that's what
        // this exercises rather than passing null directly.
        RememberMeNames names = RememberMeNames.from("", "");

        assertThat(names.parameterName()).isEqualTo("remember-me");
        assertThat(names.cookieName()).isEqualTo("remember-me");
    }

    @Test
    void blankConfiguredValueAlsoFallsBackNotJustAnAbsentOne() {
        // A property line present but left empty/whitespace must degrade
        // the same way an absent one does - not silently become "" as the
        // actual DSL parameter/cookie name.
        RememberMeNames names = RememberMeNames.from("   ", "\t");

        assertThat(names.parameterName()).isEqualTo("remember-me");
        assertThat(names.cookieName()).isEqualTo("remember-me");
    }

    @Test
    void nullConfiguredValueFallsBackToo() {
        RememberMeNames names = RememberMeNames.from(null, null);

        assertThat(names.parameterName()).isEqualTo("remember-me");
        assertThat(names.cookieName()).isEqualTo("remember-me");
    }

    @Test
    void oneConfiguredAndOneMissingResolveIndependently() {
        // Proves the two names are genuinely independent - only the
        // configured one is honored, the other still falls back on its own.
        RememberMeNames names = RememberMeNames.from("keep-me", "");

        assertThat(names.parameterName()).isEqualTo("keep-me");
        assertThat(names.cookieName()).isEqualTo("remember-me");
    }

}
