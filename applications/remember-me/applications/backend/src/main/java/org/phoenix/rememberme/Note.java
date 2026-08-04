package org.phoenix.rememberme;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A user's personal note (UC-006). Ownership is recorded as the owning
 * account's username rather than a JPA relation to {@link User}: the two
 * are equivalent for this lab (usernames are unique and never renamed) and
 * this sidesteps ever accidentally serializing a {@link User} - password
 * hash included - into an API response. {@link NoteRepository}'s
 * owner-scoped queries are what actually enforce BR-008.
 */
@Entity
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "owner_username", nullable = false, updatable = false)
    private String ownerUsername;

    protected Note() {
        // JPA
    }

    public Note(String title, String content, String ownerUsername) {
        this.title = title;
        this.content = content;
        this.ownerUsername = ownerUsername;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

}
