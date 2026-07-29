package com.fnpis.service;

import com.fnpis.domain.PortfolioValuationSnapshot;
import com.fnpis.repository.PortfolioValuationSnapshotRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes one portfolio's closing valuation, in its own transaction.
 *
 * <p>A separate bean from {@link ValuationSnapshotService} because the loop there
 * calls this once per portfolio: a self-call would not pass through Spring's
 * proxy, so {@code @Transactional} would silently not apply and every write would
 * run outside a transaction. Same reason {@code SentimentPersistenceService} is
 * its own bean.
 */
@Service
public class ValuationSnapshotWriter {

    private static final Logger log = LoggerFactory.getLogger(ValuationSnapshotWriter.class);

    private final PortfolioValuationSnapshotRepository snapshots;
    private final HoldingValuationLoader loader;

    ValuationSnapshotWriter(
            PortfolioValuationSnapshotRepository snapshots, HoldingValuationLoader loader) {
        this.snapshots = snapshots;
        this.loader = loader;
    }

    /**
     * Values one portfolio and stores the total for the session.
     *
     * <p>Overwrites an existing row rather than skipping it, the opposite of the
     * sentiment write's insert-only rule. A re-run recomputes the same close from
     * the same stored quotes - arithmetic, not a fresh opinion - so the later
     * value is the more correct one. The composite key {@code (portfolio_id,
     * snapshot_date)} makes that an update rather than a duplicate.
     *
     * @return false when the portfolio could not be valued at all
     */
    @Transactional
    public boolean capture(Long portfolioId, LocalDate session, Instant now) {
        Valuation valued = loader.value(portfolioId, now);
        BigDecimal total = valued.totalMarketValue();
        if (total == null) {
            log.debug("No valuation for portfolio {} on {}", portfolioId, session);
            return false;
        }
        // An empty portfolio still gets a zero row (EC-01): zero is what it was
        // worth, and a gap in the line would read as a failed job instead.
        snapshots.save(new PortfolioValuationSnapshot(portfolioId, session, total));
        return true;
    }
}
