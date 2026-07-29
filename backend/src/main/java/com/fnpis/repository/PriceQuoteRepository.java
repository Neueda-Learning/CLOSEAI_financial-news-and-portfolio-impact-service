package com.fnpis.repository;

import com.fnpis.domain.PriceQuote;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Latest quotes. Module B owns writes here; module A only reads.
 *
 * <p>Reads never reach a provider (decision 2) - whatever the refresh job last
 * landed is what valuation uses, and its age is reported as
 * {@code asOf}/{@code stale} rather than hidden.
 *
 * <p>Expect this table to be empty until module B's refresh job ships. That is
 * the EC-13 path, not a failure: holdings come back with
 * {@code quoteAvailable: false}.
 */
@Repository
public interface PriceQuoteRepository extends JpaRepository<PriceQuote, String> {

    /** Quotes for the symbols a portfolio holds, in one query rather than one per row. */
    List<PriceQuote> findBySymbolIn(Collection<String> symbols);
}
