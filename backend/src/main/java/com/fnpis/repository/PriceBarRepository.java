package com.fnpis.repository;

import com.fnpis.domain.PriceBar;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Daily closes (module B's snapshot job). Module E reads two of them to derive
 * the session return r (requirements 5.3 step 5, tier 1).
 *
 * <p>r for attribution date d is {@code (close_d - close_prev) / close_prev}.
 * "prev" is the last session that actually has a bar, not literally d-1 - a
 * three-day weekend leaves no Saturday or Sunday row, and the return is still
 * measured against Friday. Asking for "the two most recent bars up to and
 * including d" lets the query, not the caller, decide what the prior session
 * was, so holidays and gaps need no calendar here (matching
 * {@link com.fnpis.service.AttributionDateResolver}, which also carries no
 * holiday table).
 *
 * <p>Fewer than two rows, or a newest row that is not d itself, is the EC-18
 * path: with no close on d and its prior session there is no return, and the
 * assessment degrades to INCONCLUSIVE rather than inventing a zero.
 */
@Repository
public interface PriceBarRepository extends JpaRepository<PriceBar, PriceBar.Key> {

    /**
     * The two most recent bars for one symbol on or before {@code asOf}, newest
     * first. Element 0 is d's own close when d traded; element 1 is the true
     * prior session, whatever gap precedes it.
     *
     * <p>Returns a list rather than the ratio so the repository stays free of
     * arithmetic (CLAUDE.md layering table): the service pairs the two closes
     * and computes r, the repository only reads.
     */
    List<PriceBar> findTop2BySymbolAndTradeDateLessThanEqualOrderByTradeDateDesc(
            String symbol, LocalDate asOf);
}
