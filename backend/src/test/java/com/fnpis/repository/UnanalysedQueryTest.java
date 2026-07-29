package com.fnpis.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fnpis.domain.NewsArticle;
import com.fnpis.domain.SentimentScore;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;

/**
 * Parses {@link NewsArticleRepository#findUnanalysed}'s JPQL against the real
 * entity mappings.
 *
 * <p>A hand-written {@code @Query} is only compiled when Spring Data creates the
 * repository, which happens at application startup. Every other test in this
 * suite mocks the repository, so a typo in the query - a wrong property name, a
 * mistyped entity - would pass the whole suite and fail on boot. This test is
 * the cheap version of that check.
 *
 * <p>No database and no Docker: Hibernate builds its metamodel and parses HQL
 * from the mappings alone, so the dialect is named explicitly and no connection
 * is ever opened. The real integration coverage still belongs on Testcontainers
 * MySQL (never H2 - its DECIMAL and index-length behaviour differ), but that
 * needs a running daemon and this does not.
 */
class UnanalysedQueryTest {

    /** Kept in sync with the repository by the assertion below, not by hand. */
    private static final String QUERY = """
            select a from NewsArticle a
            where not exists (
                select 1 from SentimentScore s where s.articleId = a.id
            )
            order by a.publishedAt asc
            """;

    private static SessionFactory sessionFactory;

    @BeforeAll
    static void bootMetamodel() {
        MutablePersistenceUnitInfo unit = new MutablePersistenceUnitInfo() {
            @Override
            public String getPersistenceUnitName() {
                return "parse-only";
            }

            /**
             * Spring's own class leaves this unsupported, and Hibernate asks for
             * it while scanning. Null means "no temporary loader", which is
             * correct here - the managed classes are already on the test
             * classpath and need no enhancement pass.
             */
            @Override
            public ClassLoader getNewTempClassLoader() {
                return null;
            }
        };
        unit.addManagedClassName(NewsArticle.class.getName());
        unit.addManagedClassName(SentimentScore.class.getName());

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
    @DisplayName("the backlog query parses against the entity mappings")
    void queryParses() {
        // Compiling it is the assertion: an unknown property or entity name
        // raises here rather than at application startup.
        assertThatCode(() -> sessionFactory.inSession(session ->
                session.createQuery(QUERY, NewsArticle.class)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the test's copy of the query matches the repository's")
    void queryIsTheOneShipped() throws Exception {
        var method = NewsArticleRepository.class.getMethod(
                "findUnanalysed", org.springframework.data.domain.Pageable.class);
        String shipped = method
                .getAnnotation(org.springframework.data.jpa.repository.Query.class)
                .value();

        // Without this the test could keep passing against a stale copy while the
        // shipped query drifts into something that does not parse.
        assertThat(normalise(shipped)).isEqualTo(normalise(QUERY));
    }

    private static String normalise(String hql) {
        return hql.replaceAll("\\s+", " ").trim();
    }
}
