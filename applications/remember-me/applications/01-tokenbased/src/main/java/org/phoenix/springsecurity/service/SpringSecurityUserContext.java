package org.phoenix.springsecurity.service;

import org.phoenix.springsecurity.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * An implementation of {@link UserContext} that looks up the {@link User} using the Spring Security's
 * {@link Authentication} by principal name.
 *
 * @author bnasslahsen
 *
 */
@Component
public class SpringSecurityUserContext implements UserContext {

    private static final Logger logger = LoggerFactory
            .getLogger(SpringSecurityUserContext.class);

    private final UserService userService;
    private final UserDetailsService userDetailsService;

    public SpringSecurityUserContext(final UserService userService,
                                     final UserDetailsService userDetailsService) {
        if (userService == null) {
            throw new IllegalArgumentException("userService cannot be null");
        }
        if (userDetailsService == null) {
            throw new IllegalArgumentException("userDetailsService cannot be null");
        }
        this.userService = userService;
        this.userDetailsService = userDetailsService;
    }


    @Override
    public User getCurrentUser() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        if (authentication == null) {
            return null;
        }

        String email = authentication.getName();
        if (email == null) {
            return null;
        }
        User result = userService.findUserByEmail(email);
        if (result == null) {
            throw new IllegalStateException(
                    "Spring Security is not in synch with Users. Could not find user with email " + email);
        }

        logger.info("User: {}", result);
        return result;
    }

    @Override
    public void setCurrentUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user cannot be null");
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,
                user.getPassword(), userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

}
