package com.fnpis.repository;

import com.fnpis.domain.Holding;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Holdings (A4-A7).
 *
 * <p>Reads only - no derived columns to select, because market value, P&amp;L and
 * weight are computed in the service layer from the live quote. The repository
 * layer must not compute (CLAUDE.md layering table).
 */
public interface HoldingRepository extends JpaRepository<Holding, Long> {

    /** Every position in one portfolio. Used by the valuation pass, which needs all of them to total. */
    List<Holding> findByPortfolioIdOrderBySymbol(Long portfolioId);

    /** Paged variant for the holdings list endpoint (A5). */
    Page<Holding> findByPortfolioId(Long portfolioId, Pageable pageable);

    /**
     * The single position for one symbol, if any.
     *
     * <p>EC-09 hinges on this: a repeat add must find the existing row and merge
     * into it rather than insert a second one. The unique constraint on
     * {@code (portfolio_id, symbol)} guarantees at most one match, so
     * {@code Optional} is the honest return type.
     */
    Optional<Holding> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);

    /**
     * Same lookup as a locking read, for the EC-09 merge.
     *
     * <p>{@code FOR UPDATE} here is about visibility, not just exclusion. Under
     * InnoDB's default REPEATABLE READ a plain SELECT answers from the
     * transaction's snapshot, so an add that waited on the portfolio lock could
     * still read a pre-merge quantity and compute the average against a stale
     * lot. A locking read always sees the latest committed row.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Holding h where h.portfolioId = :portfolioId and h.symbol = :symbol")
    Optional<Holding> findByPortfolioIdAndSymbolForUpdate(
            @Param("portfolioId") Long portfolioId, @Param("symbol") String symbol);

    /** How many positions a portfolio holds. Cheaper than loading them to check emptiness (EC-01). */
    long countByPortfolioId(Long portfolioId);
}
