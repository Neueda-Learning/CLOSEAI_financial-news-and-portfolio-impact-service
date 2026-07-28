package com.fnpis.repository;

import com.fnpis.domain.Security;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Securities master data, seeded by V5.
 *
 * <p>Read-only from module A's side: the watchlist does not grow at runtime in
 * the MVP (AS-03). Used to reject unknown symbols (EC-05) and to attach company
 * names to holdings rows.
 */
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
