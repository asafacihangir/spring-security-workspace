package org.phoenix.rememberme;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC-008 Account Settings. Content is deliberately minimal (viewing/editing
 * a display name) - the point of this phase is the access-control rule
 * around this page, not the account data itself.
 *
 * <p>BR-010/BR-011's actual enforcement lives in {@link SecurityConfig}
 * ({@code isFullyAuthenticated()} on {@code /api/account} and
 * {@code /api/account/**}), not here - by the time either method below
 * runs, the request has already been rejected with 401/403 if its
 * authentication level was anything less than Fully Authenticated. Nothing
 * in this controller re-checks that; there is exactly one enforcement
 * point, not a frontend guard duplicating a backend one (or vice versa).
 *
 * <p>Like {@link NoteController}, the account resolved here is always the
 * caller's own ({@code authentication.getName()}), never a client-supplied
 * id.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final UserRepository userRepository;

    public AccountController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<AccountResponse> get(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .map(user -> ResponseEntity.ok(AccountResponse.from(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<AccountResponse> update(@RequestBody AccountUpdateRequest request,
            Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .map(user -> {
                    user.setDisplayName(request.displayName());
                    return ResponseEntity.ok(AccountResponse.from(userRepository.save(user)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
