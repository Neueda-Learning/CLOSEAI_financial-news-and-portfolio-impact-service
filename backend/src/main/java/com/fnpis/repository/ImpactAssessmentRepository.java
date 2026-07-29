package com.fnpis.repository;

import com.fnpis.domain.ImpactAssessment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
}
