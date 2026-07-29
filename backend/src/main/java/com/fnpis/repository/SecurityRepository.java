package com.fnpis.repository;

import com.fnpis.domain.Security;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Read the watchlist (V5 seed).
 *
 * <p>Module A (portfolio CRUD) and Module B (quote refresh) both need this.
 * The first module to land it creates it; the second reuses it. Do not create
 * a second SecurityRepository in another package.
 *
 * <p>Read-only in the MVP: the watchlist does not grow at runtime (AS-03).
 * Module A uses it to reject unknown symbols (EC-05) and to attach company names
 * to holdings rows.
 */
@Repository
public interface SecurityRepository extends JpaRepository<Security, String> {

    /**
     * Company names for a set of symbols, in one query.
     *
     * <p>The holdings list needs a name per row. Calling {@code findById} in the
     * loop would issue one statement per position - the classic N+1. This keeps
     * it at one.
     */
    List<Security> findBySymbolIn(Collection<String> symbols);

    /**
     * Symbol prefix or company name substring, whichever matches (A4, EC-05).
     *
     * <p>The two halves are deliberately different. A symbol is short and typed
     * from the front, so "nv" should reach NVDA but "vd" should not - a substring
     * match on the code would make almost every query hit several rows out of
     * fifteen and rank noise above the intended pick. A company name is the
     * opposite: nobody types "the walt disney company" from the start, so the
     * useful match there is anywhere in the string.
     *
     * <p>The caller passes both patterns because the wildcards differ; building
     * them here would hide that asymmetry behind one argument.
     *
     * <p>{@code LOWER()} on both sides rather than relying on the column
     * collation: utf8mb4_unicode_ci happens to be case-insensitive, but a later
     * migration that changes collation would silently turn this case-sensitive,
     * and a search box that stops matching lowercase input is the kind of break
     * nobody notices in review.
     */
    @Query("""
            SELECT s FROM Security s
            WHERE LOWER(s.symbol) LIKE LOWER(:symbolPrefix)
               OR LOWER(s.companyName) LIKE LOWER(:namePattern)
            ORDER BY s.symbol
            """)
    List<Security> search(
            @Param("symbolPrefix") String symbolPrefix,
            @Param("namePattern") String namePattern,
            Pageable pageable);
}
