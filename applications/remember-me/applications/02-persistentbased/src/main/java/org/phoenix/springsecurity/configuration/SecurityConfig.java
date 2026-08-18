package org.phoenix.springsecurity.configuration;

import jakarta.servlet.http.HttpServletResponse;

import org.phoenix.springsecurity.repository.JpaPersistentTokenRepository;
import org.phoenix.springsecurity.repository.RememberMeTokenRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        return authenticationManagerBuilder.build();
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            PersistentTokenRepository persistentTokenRepository) throws Exception {
        http.authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/signup").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/work-logs").hasRole("ADMIN")
                        .anyRequest().hasAnyRole("USER", "ADMIN"))

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(
                                (request, response, ex) -> response.sendError(HttpServletResponse.SC_FORBIDDEN)))
                .formLogin(form -> form
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, auth) -> response.setStatus(HttpServletResponse.SC_OK))
                        .failureHandler(
                                (request, response, ex) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .permitAll())
                .logout(form -> form
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(
                                (request, response, auth) -> response.setStatus(HttpServletResponse.SC_OK))
                        .permitAll())
                .csrf(AbstractHttpConfigurer::disable);

        http.rememberMe(httpSecurityRememberMeConfigurer -> httpSecurityRememberMeConfigurer
                .key("jbcpCalendar")
                .tokenRepository(persistentTokenRepository));

        return http.build();
    }


    @Bean
    public PersistentTokenRepository persistentTokenRepository(RememberMeTokenRepository rmtr) {
        return new JpaPersistentTokenRepository(rmtr);
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(4);
    }

}
