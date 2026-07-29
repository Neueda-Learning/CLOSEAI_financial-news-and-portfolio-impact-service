package com.fnpis.service;

import com.fnpis.domain.Portfolio;
import com.fnpis.repository.PortfolioRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Records what each portfolio was worth at one close (B5, F5).
 *
 * <p><b>This data has exactly one chance to be captured.</b> A snapshot is the sum
 * of holding market values as they stood at that close, and past holdings are
 * recorded nowhere, so a session that goes unwritten cannot be reconstructed
 * later. The architecture calls the job urgent for that reason: F5's chart needs
 * several sessions before it draws a line at all, and it cannot be caught up the
 * week of the demo.
 *
 * <p>Values every portfolio through {@link HoldingValuationLoader}, the same path
 * the summary and holdings endpoints use. A second valuation implementation here
 * would eventually disagree with the screens, which is what SC-002 catches.
 *
 * <p>Not {@code @Transactional}: the loop spans every portfolio, and one
 * transaction around all of them would roll back snapshots already taken when a
 * later portfolio failed. Today's close does not come back.
 */
@Service
public class ValuationSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(ValuationSnapshotService.class);

    private final PortfolioRepository portfolios;
    private final ValuationSnapshotWriter writer;

    ValuationSnapshotService(PortfolioRepository portfolios, ValuationSnapshotWriter writer) {
        this.portfolios = portfolios;
        this.writer = writer;
    }

    /**
     * Writes one snapshot per portfolio for the given session.
     *
     * <p>A portfolio that throws is logged and skipped rather than aborting the
     * run: one portfolio with unusable quotes must not cost every other portfolio
     * its only chance at this session.
     *
     * @return how many snapshots were written
     */
    public int captureAll(LocalDate session, Instant now) {
        int written = 0;
        int failed = 0;
        for (Portfolio portfolio : portfolios.findAll()) {
            try {
                if (writer.capture(portfolio.getId(), session, now)) {
                    written++;
                }
            } catch (RuntimeException e) {
                failed++;
                // The exception type only. A provider message can carry values
                // that do not belong in the log.
                log.warn("Valuation snapshot failed for portfolio {} on {} - skipping ({})",
                        portfolio.getId(), session, e.getClass().getSimpleName());
            }
        }
        log.info("Valuation snapshot for {} complete: {} written, {} failed", session, written, failed);
        return written;
    }
}
