package org.phoenix.rememberme;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * BR-008 (note ownership) is enforced right here: every lookup is scoped to
 * the requesting user's username, so a note belonging to someone else is
 * indistinguishable from a note that doesn't exist at all - callers in
 * {@link NoteController} never see another user's note by any path.
 */
public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByOwnerUsernameOrderByIdDesc(String ownerUsername);

    Optional<Note> findByIdAndOwnerUsername(Long id, String ownerUsername);

}
