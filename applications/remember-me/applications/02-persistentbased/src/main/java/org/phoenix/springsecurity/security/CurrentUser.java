package org.phoenix.springsecurity.security;

import org.phoenix.springsecurity.domain.User;
import org.phoenix.springsecurity.service.UserService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


@Component
public class CurrentUser {

    private final UserService userService;

    public CurrentUser(final UserService userService) {
        this.userService = userService;
    }

    public User get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return null;
        }

        String email = authentication.getName();
        User user = userService.findUserByEmail(email);
        if (user == null) {
            throw new IllegalStateException(
                    "Spring Security is not in synch with Users. Could not find user with email " + email);
        }
        return user;
    }

}
