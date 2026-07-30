package com.fnpis.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fnpis.domain.Security;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;

/**
 * Parses {@link SecurityRepository#search}'s JPQL against the real mappings.
 *
 * <p>A hand-written {@code @Query} is only compiled when Spring Data builds the
 * repository, at application startup. {@code SecurityServiceTest} mocks the
 * repository, so a malformed query there passes every assertion and fails on
 * boot instead.
 *
 * <p>That is not hypothetical for this query. The ESCAPE clause sits in a text
 * block, which still processes escape sequences, so the number of backslashes to
 * write is genuinely easy to get wrong - and Hibernate is strict: it demands
 * exactly one character and rejects a longer literal outright. This test caught
 * that mistake while the clause was being added, rather than at startup.
 *
 * <p>No database and no Docker: Hibernate parses HQL from the mappings alone, so
 * the dialect is named explicitly and no connection is opened. Real integration
 * coverage still belongs on Testcontainers MySQL (never H2 - its DECIMAL and
 * index-length behaviour differ from the real thing).
 */
@DisplayName("Security search JPQL")
class SecuritySearchQueryTest {

    private static SessionFactory sessionFactory;

    @BeforeAll
    static void bootMetamodel() {
        MutablePersistenceUnitInfo unit = new MutablePersistenceUnitInfo() {
            @Override
            public String getPersistenceUnitName() {
                return "parse-only";
            }

            /**
             * Spring's own class leaves this unsupported and Hibernate asks for
             * it while scanning. Null means "no temporary loader", which is right
             * here - the managed class is already on the test classpath.
             */
            @Override
            public ClassLoader getNewTempClassLoader() {
                return null;
            }
        };
        unit.addManagedClassName(Security.class.getName());

        sessionFactory = Bootstrap.getEntityManagerFactoryBuilder(
                        unit,
                        Map.of(
                                // Named rather than detected: detection needs a
                                // live connection, and this test has none.
                                AvailableSettings.DIALECT, MySQLDialect.class.getName(),
                                AvailableSettings.HBM2DDL_AUTO, "none",
                                AvailableSettings.ALLOW_UPDATE_OUTSIDE_TRANSACTION, "false"))
                .build()
                .unwrap(SessionFactory.class);
    }

    @AfterAll
    static void close() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    @DisplayName("parses against the entity mappings")
    void queryParses() {
        // Compiling it is the assertion: an unknown property, a mistyped entity,
        // or a malformed ESCAPE raises here rather than at application startup.
        assertThatCode(() -> sessionFactory.inSession(session ->
                session.createQuery(shippedQuery(), Security.class)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("declares a backslash as the LIKE escape character")
    void escapeCharacterIsABackslash() {
        String shipped = shippedQuery();

        // One backslash is what the annotation holds at runtime, and it has to
        // agree with what escapeWildcards() prefixes its metacharacters with. If
        // one side is ever changed alone, the escaping stops matching and this
        // fails - which the parse test above would not catch, since any valid
        // single character parses.
        assertThat(shipped).contains("ESCAPE '\\'");
    }

    @Test
    @DisplayName("escapes both patterns, so neither column loses the guard")
    void bothPatternsAreEscaped() {
        String shipped = shippedQuery();

        // The service escapes one query string and hands it to both parameters.
        // An ESCAPE on only one branch would leave the other silently unguarded.
        assertThat(shipped.split("ESCAPE", -1)).hasSize(3);
    }

    private static String shippedQuery() throws AssertionError {
        try {
            return SecurityRepository.class
                    .getMethod("search", String.class, String.class, Pageable.class)
                    .getAnnotation(Query.class)
                    .value();
        } catch (NoSuchMethodException e) {
            throw new AssertionError("SecurityRepository.search signature changed", e);
        }
    }
}
