package com.fnpis.repository;

import com.fnpis.domain.PortfolioValuationSnapshot;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The portfolio value history behind F5's line chart (B5).
 *
 * <p>This table cannot be backfilled. A snapshot is the sum of holding market
 * values as they stood at one close, and the holdings as they stood on a past
 * date are recorded nowhere - so a session that goes unwritten is gone. That is
 * why the write lives in the closing job rather than waiting for the read
 * endpoint to be built.
 */
@Repository
public interface PortfolioValuationSnapshotRepository
        extends JpaRepository<PortfolioValuationSnapshot, PortfolioValuationSnapshot.Key> {

    /**
     * One portfolio's snapshots across a closed date range, oldest first (F5).
     *
     * <p>Ascending because Chart.js plots the array in order and would draw the
     * line backwards otherwise - the same reason {@code PricePointRepository}
     * sorts ascending.
     *
     * <p>Bounded by the range rather than returning the whole history: this table
     * grows by one row per portfolio per session, so an unbounded read gets
     * slower every day the job runs.
     */
    List<PortfolioValuationSnapshot> findByPortfolioIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            Long portfolioId, LocalDate from, LocalDate to);
}
