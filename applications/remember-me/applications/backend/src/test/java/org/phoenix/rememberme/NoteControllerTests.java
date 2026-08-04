package org.phoenix.rememberme;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Test Adimi 4: automated coverage for UC-006's main scenario (create/list),
 * A1 (update), A2 (delete), A3 (empty title rejected server-side), and
 * BR-008 (a second user can never read, update, or delete the first user's
 * note - not even to learn that it exists).
 *
 * <p>BR-008 only means something with two real accounts, so this test
 * creates a second user directly through {@link UserRepository} in
 * {@link #ensureSecondUserExists()} - deliberately not through
 * {@link DemoUserSeeder}, which stays scoped to seeding the one demo
 * account used by the real login demo (see task-2-brief.md). Both users
 * carry the single {@code ROLE_USER} authority, same as every account in
 * this lab (BR-001/C-007 is about role hierarchy, not user count).
 *
 * <p>Runs against the real MySQL instance (task infra:up), same as
 * {@link FormLoginTests}. Notes created here are cleaned up in
 * {@link #deleteNotesCreatedDuringTest()} so repeated runs don't accumulate
 * rows in the shared dev database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NoteControllerTests {

    private static final String OTHER_USERNAME = "note-test-second-user";
    private static final String OTHER_PASSWORD = "password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> createdNoteIds = new ArrayList<>();

    @BeforeEach
    void ensureSecondUserExists() {
        if (userRepository.findByUsername(OTHER_USERNAME).isEmpty()) {
            userRepository.save(new User(OTHER_USERNAME, passwordEncoder.encode(OTHER_PASSWORD)));
        }
    }

    @AfterEach
    void deleteNotesCreatedDuringTest() {
        createdNoteIds.forEach(noteRepository::deleteById);
        createdNoteIds.clear();
    }

    private MockHttpSession loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(formLogin("/api/login").user(username).password(password))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private long createNote(MockHttpSession session, String title, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/notes").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NoteRequest(title, content))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        createdNoteIds.add(id);
        return id;
    }

    @Test
    void ownerCanCreateListUpdateAndDeleteTheirOwnNote() throws Exception {
        MockHttpSession session = loginAs(DemoUserSeeder.DEMO_USERNAME, DemoUserSeeder.DEMO_PASSWORD);

        long id = createNote(session, "Alisveris listesi", "Ekmek, sut");

        // Main scenario step 6: shows up in the owner's list.
        mockMvc.perform(get("/api/notes").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].title").value("Alisveris listesi"));

        mockMvc.perform(get("/api/notes/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Alisveris listesi"));

        // A1: update.
        mockMvc.perform(put("/api/notes/" + id).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NoteRequest("Guncellenmis baslik", "Yumurta"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Guncellenmis baslik"))
                .andExpect(jsonPath("$.content").value("Yumurta"));

        // A2: delete, then confirm it is gone.
        mockMvc.perform(delete("/api/notes/" + id).session(session))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/notes/" + id).session(session))
                .andExpect(status().isNotFound());

        createdNoteIds.remove(id); // already deleted by the assertions above
    }

    @Test
    void a3EmptyTitleIsRejectedServerSideOnCreateAndUpdate() throws Exception {
        MockHttpSession session = loginAs(DemoUserSeeder.DEMO_USERNAME, DemoUserSeeder.DEMO_PASSWORD);

        int notesBefore = noteRepository.findByOwnerUsernameOrderByIdDesc(DemoUserSeeder.DEMO_USERNAME).size();

        mockMvc.perform(post("/api/notes").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NoteRequest("   ", "content"))))
                .andExpect(status().isBadRequest());

        // Nothing was persisted by the rejected request.
        org.assertj.core.api.Assertions.assertThat(
                        noteRepository.findByOwnerUsernameOrderByIdDesc(DemoUserSeeder.DEMO_USERNAME))
                .hasSize(notesBefore);

        long id = createNote(session, "Gecerli baslik", "content");
        mockMvc.perform(put("/api/notes/" + id).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NoteRequest("", "yeni icerik"))))
                .andExpect(status().isBadRequest());

        // Rejected update must not have changed the stored note.
        mockMvc.perform(get("/api/notes/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Gecerli baslik"));
    }

    @Test
    void br008AnotherUserCannotReadUpdateOrDeleteSomeoneElsesNote() throws Exception {
        MockHttpSession ownerSession = loginAs(DemoUserSeeder.DEMO_USERNAME, DemoUserSeeder.DEMO_PASSWORD);
        long id = createNote(ownerSession, "Ozel not", "Sadece sahibi gormeli");

        MockHttpSession otherSession = loginAs(OTHER_USERNAME, OTHER_PASSWORD);

        mockMvc.perform(get("/api/notes/" + id).session(otherSession))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/notes/" + id).session(otherSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NoteRequest("Ele gecirilmis", "x"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/notes/" + id).session(otherSession))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/notes").session(otherSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").doesNotExist());

        // Untouched by the other user's rejected attempts.
        mockMvc.perform(get("/api/notes/" + id).session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Ozel not"));
    }

}
