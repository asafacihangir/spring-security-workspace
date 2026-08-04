package org.phoenix.rememberme;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Faz 6 (UC-010, FR-010): schema-management-only mapping for the
 * {@code persistent_logins} table Spring Security's
 * {@code JdbcTokenRepositoryImpl} expects. Column names/types mirror that
 * class's own {@code CREATE_TABLE_SQL} constant exactly (that layout is the
 * library's contract, not this app's choice):
 * {@code username varchar(64) not null, series varchar(64) primary key,
 * token varchar(64) not null, last_used timestamp not null}.
 *
 * <p><b>Why an {@code @Entity} for a table nothing here reads or writes via
 * JPA:</b> {@code users} and {@code notes} both get their schema from
 * {@code spring.jpa.hibernate.ddl-auto=update} inferring DDL off
 * {@code @Entity} classes (see {@link User}, {@link Note}). Reusing that
 * same mechanism here - rather than
 * {@code JdbcTokenRepositoryImpl.setCreateTableOnStartup(true)}, which reruns
 * a bare {@code CREATE TABLE} (no {@code IF NOT EXISTS}) on every boot and
 * would fail the second restart, or introducing a separate schema.sql/
 * migration mechanism this project has no other precedent for - keeps
 * exactly one schema-management story for the whole app, matching its
 * existing convention instead of adding a second one just for this table.
 *
 * <p>Crucially for BR-013 (config-only strategy switching): this class is
 * unconditional. It sits on the classpath and gets entity-scanned/DDL'd
 * regardless of {@code app.remember-me.strategy} (see
 * {@link SecurityConfig}), so {@code persistent_logins} exists identically
 * whichever strategy is active - table creation itself never depends on
 * which strategy is currently selected, only the runtime reads/writes do.
 * Those reads/writes are done entirely by Spring Security's own
 * {@code JdbcTokenRepositoryImpl} (see
 * {@link SecurityConfig#persistentTokenRepository}) operating on raw JDBC
 * against the columns declared below - this class itself is never queried
 * or saved by application code; it exists purely so Hibernate's DDL
 * generation sees the table. (Its unusual "entity with no repository,
 * mutated only by a third party underneath it" shape is a deliberate
 * consequence of that split, not an oversight.)
 */
@Entity
@Table(name = "persistent_logins")
public class PersistentLogin {

    @Id
    @Column(length = 64, nullable = false)
    private String series;

    @Column(length = 64, nullable = false)
    private String username;

    @Column(length = 64, nullable = false)
    private String token;

    @Column(name = "last_used", nullable = false, columnDefinition = "timestamp")
    private Instant lastUsed;

    protected PersistentLogin() {
        // JPA / schema-inference only - see class javadoc. No application
        // code ever constructs or persists this entity directly; Spring
        // Security's JdbcTokenRepositoryImpl reads/writes the underlying
        // table over plain JDBC instead.
    }

}
