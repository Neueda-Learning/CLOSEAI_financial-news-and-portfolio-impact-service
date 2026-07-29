package com.fnpis.repository;

import com.fnpis.domain.Portfolio;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * The portfolio row, locked {@code FOR UPDATE} (EC-09).
     *
     * <p>Serialises position writes against one portfolio. Adding a holding is a
     * read-modify-write - find the existing position, merge, save - and two of
     * those interleaving break in two ways: concurrent first-adds of one symbol
     * collide on {@code uq_holding_portfolio_symbol}, and concurrent adds to an
     * <i>existing</i> position both read the same quantity and the second save
     * silently discards the first. The parent row is the natural lock because it
     * exists whether or not the position does; locking the position row cannot
     * guard its own insertion.
     *
     * <p>The lock is held to commit. One row, always taken in the same order, so
     * no deadlock cycle is reachable.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Portfolio p where p.id = :id")
    Optional<Portfolio> findByIdForUpdate(@Param("id") Long id);
}
