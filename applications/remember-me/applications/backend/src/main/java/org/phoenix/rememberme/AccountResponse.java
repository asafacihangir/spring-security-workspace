package org.phoenix.rememberme;

/** UC-008 Account Settings payload - deliberately minimal (see AccountController). */
public record AccountResponse(String username, String displayName) {

    static AccountResponse from(User user) {
        return new AccountResponse(user.getUsername(), user.getDisplayName());
    }

}
