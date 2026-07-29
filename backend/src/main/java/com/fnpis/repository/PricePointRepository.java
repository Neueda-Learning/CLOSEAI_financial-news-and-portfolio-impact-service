package com.fnpis.repository;

import com.fnpis.domain.PricePoint;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PricePointRepository
        extends JpaRepository<PricePoint, PricePoint.Key> {

    /**
     * One symbol's intraday points inside a window, oldest first (F4).
     *
     * <p>Feeds the linked view's chart. Ascending because Chart.js plots the array
     * in order and would draw the line backwards otherwise.
     *
     * <p>Bounded by the window rather than returning the whole symbol's history:
     * the table grows by one row per symbol per refresh, so an unbounded read
     * would get slower every day the quote job runs.
     */
    List<PricePoint> findBySymbolAndCapturedAtBetweenOrderByCapturedAtAsc(
            String symbol, Instant from, Instant to);
}
