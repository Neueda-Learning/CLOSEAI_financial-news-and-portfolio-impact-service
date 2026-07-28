package com.fnpis.repository;

import com.fnpis.domain.Portfolio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Portfolios (A1-A3).
 *
 * <p>No {@code findByUserId}: the system is single-user (AS-01), so every
 * portfolio in the table belongs to the one operator.
 */
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    /**
     * All portfolios, newest first.
     *
     * <p>A2 renders this as a list. Creation order is the only ordering that
     * does not change under the user's feet - sorting by total value would
     * reshuffle the list on every price refresh.
     */
    List<Portfolio> findAllByOrderByCreatedAtDesc();
}
