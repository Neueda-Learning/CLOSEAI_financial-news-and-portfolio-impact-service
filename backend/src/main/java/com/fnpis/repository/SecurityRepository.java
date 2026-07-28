package com.fnpis.repository;

import com.fnpis.domain.Security;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
