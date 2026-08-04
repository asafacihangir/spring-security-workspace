package org.phoenix.rememberme;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Faz 6 (UC-010 A1): end-to-end proof - not just the unit-level
 * {@link RememberMeStrategyTests} - that an unrecognized
 * {@code app.remember-me.strategy} value actually fails application
 * startup, rather than being silently ignored in favor of some default
 * strategy.
 *
 * <p>{@link SecurityConfig#filterChain} is a singleton {@code @Bean}
 * method, so Spring instantiates it eagerly while refreshing the
 * {@code ApplicationContext} - the {@link IllegalStateException}
 * {@link RememberMeStrategy#from} throws for a bad value therefore surfaces
 * here as a {@link BeanCreationException} out of
 * {@code SpringApplication.run(...)} itself, before
 * {@link DemoUserSeeder}'s {@code CommandLineRunner} (which only runs after
 * a successful context refresh) ever touches the database - this boots the
 * real application against the real MySQL instance (task infra:up) but
 * writes nothing to it.
 */
class RememberMeApplicationFailsFastOnInvalidStrategyTests {

    @Test
    void unrecognizedStrategyValueFailsApplicationStartup() {
        // Command-line args (SpringApplication.run(String...)), not
        // SpringApplicationBuilder#properties(...) - the latter registers
        // "default properties" (lowest precedence in Spring Boot's property
        // source ordering), which application.properties' own
        // app.remember-me.strategy=token would silently win over, defeating
        // the point of this test. Command-line args sit above every file
        // source, so "bogus" is what SecurityConfig#filterChain actually
        // sees here.
        assertThatThrownBy(() -> new SpringApplicationBuilder(RememberMeApplication.class)
                .run("--app.remember-me.strategy=bogus", "--server.port=0"))
                .isInstanceOf(BeanCreationException.class)
                .rootCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bogus");
    }

}
