package com.fnpis.repository;

import com.fnpis.domain.Alignment;
import com.fnpis.domain.ImpactAssessment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Assessed impacts (E1-E4). Module E owns writes here; the read endpoints and
 * the linked view read back.
 *
 * <p>Recompute is an idempotent upsert, not a delete-then-insert. The schema's
 * {@code uq_impact_article_symbol_portfolio_date} makes at most one row per
 * (article, symbol, portfolio, session), so a re-run for the same day finds the
 * existing row through {@link #findByArticleIdAndSymbolAndPortfolioIdAndAttributionDate}
 * and overwrites it in place. Deleting first would leave a hole in the day's
 * data if the run failed halfway (V4 migration comment).
 */
public interface ImpactAssessmentRepository extends JpaRepository<ImpactAssessment, Long> {

    /**
     * The one assessment for a story's effect on a symbol in a portfolio on a
     * session, if it has already been computed. Empty on first run; present on
     * every recompute, which is what makes the write an update rather than a
     * duplicate.
     */
    Optional<ImpactAssessment> findByArticleIdAndSymbolAndPortfolioIdAndAttributionDate(
            Long articleId, String symbol, Long portfolioId, LocalDate attributionDate);

    /** Every assessment for one portfolio on one session (E1-E3, E5 read path). */
    List<ImpactAssessment> findByPortfolioIdAndAttributionDate(
            Long portfolioId, LocalDate attributionDate);

    /** Whether any assessment exists for this article (drives hasImpact in news lists). */
    boolean existsByArticleId(Long articleId);

    /**
     * One session's assessments for a portfolio, newest-written first and paged
     * (E1-E3 list endpoint).
     *
     * <p>Ordering by {@code computedAt} rather than by impact size on purpose:
     * the service sorts by magnitude when it needs to, and a stable database
     * order keeps pagination from repeating or dropping rows between pages the
     * way an order-by-computed-value would.
     */
    Page<ImpactAssessment> findByPortfolioIdAndAttributionDateOrderByComputedAtDesc(
            Long portfolioId, LocalDate attributionDate, Pageable page);

    /** The same page narrowed to one alignment, for the contract's filter. */
    Page<ImpactAssessment> findByPortfolioIdAndAttributionDateAndAlignmentOrderByComputedAtDesc(
            Long portfolioId, LocalDate attributionDate, Alignment alignment, Pageable page);

    /**
     * Every assessment a story produced in one portfolio, across all symbols it
     * touched (EC-24). Backs the linked view's {@code impacts} array and its
     * {@code impactedSymbols} list.
     */
    List<ImpactAssessment> findByArticleIdAndPortfolioId(Long articleId, Long portfolioId);
}
