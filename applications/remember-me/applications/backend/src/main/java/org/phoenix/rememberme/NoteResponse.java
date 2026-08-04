package org.phoenix.rememberme;

/**
 * Note representation returned to the client. Deliberately excludes the
 * owner field - the client only ever sees its own notes and never needs to
 * know how ownership is stored (BR-008 enforcement happens server-side, in
 * {@link NoteRepository} and {@link NoteController}).
 */
public record NoteResponse(Long id, String title, String content) {

    static NoteResponse from(Note note) {
        return new NoteResponse(note.getId(), note.getTitle(), note.getContent());
    }

}
