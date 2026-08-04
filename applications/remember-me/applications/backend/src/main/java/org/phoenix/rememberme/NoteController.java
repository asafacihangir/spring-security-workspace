package org.phoenix.rememberme;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC-006 CRUD for the authenticated user's own notes.
 *
 * <p>Every method resolves the current user from {@link Authentication}
 * (never from a client-supplied id), and every lookup of an existing note
 * goes through {@link NoteRepository}'s owner-scoped queries (BR-008) - a
 * note that exists but belongs to someone else returns 404, exactly like a
 * note that doesn't exist, so a caller can never tell the two apart.
 *
 * <p>Title validation (A3) happens here, server-side, regardless of
 * whatever the React form already checked client-side.
 */
@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteRepository noteRepository;

    public NoteController(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @GetMapping
    public List<NoteResponse> list(Authentication authentication) {
        return noteRepository.findByOwnerUsernameOrderByIdDesc(authentication.getName())
                .stream()
                .map(NoteResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteResponse> get(@PathVariable Long id, Authentication authentication) {
        return noteRepository.findByIdAndOwnerUsername(id, authentication.getName())
                .map(note -> ResponseEntity.ok(NoteResponse.from(note)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody NoteRequest request, Authentication authentication) {
        String validationError = validate(request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", validationError));
        }
        Note note = new Note(request.title().trim(), request.content(), authentication.getName());
        note = noteRepository.save(note);
        return ResponseEntity.status(HttpStatus.CREATED).body(NoteResponse.from(note));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody NoteRequest request,
            Authentication authentication) {
        String validationError = validate(request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", validationError));
        }
        return noteRepository.findByIdAndOwnerUsername(id, authentication.getName())
                .map(note -> {
                    note.setTitle(request.title().trim());
                    note.setContent(request.content());
                    return ResponseEntity.ok(NoteResponse.from(noteRepository.save(note)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        return noteRepository.findByIdAndOwnerUsername(id, authentication.getName())
                .map(note -> {
                    noteRepository.delete(note);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** A3: an empty/blank title is rejected regardless of client-side checks. */
    private String validate(NoteRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            return "Başlık zorunludur.";
        }
        return null;
    }

}
