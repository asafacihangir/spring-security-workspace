package org.phoenix.rememberme;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A registered account. Faz 1 only has a single demo account and a single
 * implicit {@code ROLE_USER} authority (BR-001/C-007: no role hierarchy),
 * so there is no separate roles table or column - the authority is assigned
 * in {@link AppUserDetailsService}.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    /** BCrypt hash - never a plaintext password (NFR-002). */
    @Column(nullable = false)
    private String password;

    protected User() {
        // JPA
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}
