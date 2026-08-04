package org.phoenix.rememberme;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Faz 1: session-based form login (UC-001).
 *
 * <p>The React SPA posts credentials to {@code /api/login} as a regular
 * {@code x-www-form-urlencoded} body, which Spring Security's built-in
 * {@code UsernamePasswordAuthenticationFilter} consumes directly - no
 * custom filter needed. Success/failure handlers reply with plain JSON
 * instead of the default redirects, since the frontend is an API client,
 * not a page Spring Security renders.
 *
 * <p>Unauthenticated access to any other endpoint gets a bare 401 (not a
 * redirect to a login page, and not an HTTP Basic challenge - the latter
 * makes browsers pop up their native auth dialog and hang the calling
 * {@code fetch()}, as found during Faz 0).
 *
 * <p>CSRF protection is out of scope for this lab (no requirement in
 * requirements.md/plan.md covers it across any phase); it is disabled here
 * rather than half-implemented.
 */
@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        // Strength 12 comfortably clears the NFR-002 floor of >= 10.
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginProcessingUrl("/api/login")
                        .successHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setCharacterEncoding("UTF-8");
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"username\":\"" + authentication.getName() + "\"}");
                        })
                        .failureHandler((request, response, exception) -> {
                            // BR-002: one generic message, regardless of whether the
                            // username or the password was wrong.
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setCharacterEncoding("UTF-8");
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"error\":\"Kullanıcı adı veya şifre hatalı.\"}");
                        })
                        .permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

}
