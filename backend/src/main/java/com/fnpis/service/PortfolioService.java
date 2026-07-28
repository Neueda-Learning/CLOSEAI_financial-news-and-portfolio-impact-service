package com.fnpis.service;

import com.fnpis.api.internal.dto.CreatePortfolioRequest;
import com.fnpis.api.internal.dto.PortfolioResponse;
import com.fnpis.api.internal.dto.PortfolioSummaryResponse;
import com.fnpis.common.error.ApiException;
import com.fnpis.domain.Portfolio;
import com.fnpis.repository.PortfolioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Portfolio lifecycle and valuation summaries (A1-A3, B2, B3).
 *
 * <p>Owns the two endpoints the contract tags B2/B3. The arithmetic belongs with
 * the positions it reads rather than with the job that fetches prices: module B
 * only has to land quotes, and A2's "list shows total value" then does not wait
 * on module B shipping.
 */
@Service
public class PortfolioService {

    private final PortfolioRepository portfolios;
    private final HoldingValuationLoader loader;

    PortfolioService(PortfolioRepository portfolios, HoldingValuationLoader loader) {
        this.portfolios = portfolios;
        this.loader = loader;
    }

    /** A1. */
    @Transactional
    public PortfolioResponse create(CreatePortfolioRequest request) {
        Portfolio p = new Portfolio(request.name().trim());
        p.setBaseCurrency(request.baseCurrencyOrDefault());
        Portfolio saved = portfolios.save(p);
        // A new portfolio holds nothing, so there is no quote to be stale about.
        return new PortfolioResponse(
                saved.getId(), saved.getName(), saved.getBaseCurrency(),
                new BigDecimal("0.00"), 0L, saved.getCreatedAt(), null, false);
    }

    /** A2. */
    @Transactional(readOnly = true)
    public List<PortfolioResponse> list(Instant now) {
        return portfolios.findAllByOrderByCreatedAtDesc().stream()
                .map(p -> {
                    Valuation v = loader.value(p.getId(), now);
                    return new PortfolioResponse(
                            p.getId(), p.getName(), p.getBaseCurrency(),
                            v.totalMarketValue(), v.positions().size(),
                            p.getCreatedAt(), v.asOf(), v.stale());
                })
                .toList();
    }

    /** A2 detail plus B2/B3 figures. */
    @Transactional(readOnly = true)
    public PortfolioSummaryResponse summary(Long portfolioId, Instant now) {
        Portfolio p = require(portfolioId);
        Valuation v = loader.value(portfolioId, now);
        BigDecimal pnl = v.unrealizedPnL();

        return new PortfolioSummaryResponse(
                p.getId(), p.getName(), p.getBaseCurrency(),
                v.totalMarketValue(), v.totalCost(), pnl,
                ValuationService.pctOrNull(pnl, v.totalCost()),
                v.totalDayChange(), dayChangePct(v),
                v.allocations(),
                v.asOf(), v.stale());
    }

    /**
     * A3. Positions go with it: the {@code holding} FK cascades on delete, so one
     * statement suffices and no orphan rows survive a partial failure.
     */
    @Transactional
    public void delete(Long portfolioId) {
        portfolios.delete(require(portfolioId));
    }

    /**
     * Position weights and market values, for the impact engine (requirements 5.3).
     *
     * <p>Supplies two of the four inputs that section needs - {@code w} and the
     * market value {@code valueImpact} multiplies. Exists so module E does not
     * recompute them: two implementations of the same weight would eventually
     * disagree, and SC-002/SC-003 check exactly that.
     *
     * <p>Weights are BigDecimal here rather than the Double the API returns, since
     * the caller multiplies them into money (architecture 6.2).
     *
     * @throws com.fnpis.common.error.ApiException 404 when the portfolio is gone
     */
    @Transactional(readOnly = true)
    public PortfolioWeights weights(Long portfolioId, Instant now) {
        require(portfolioId);
        return loader.value(portfolioId, now).weights();
    }

    /** Shared 404 so every endpoint reports a missing portfolio identically. */
    public Portfolio require(Long portfolioId) {
        return portfolios.findById(portfolioId)
                .orElseThrow(() -> ApiException.portfolioNotFound(portfolioId));
    }

    private static Double dayChangePct(Valuation v) {
        if (v.totalDayChange() == null) {
            return null;
        }
        // Against the portfolio's previous close, which is today's value less
        // today's move.
        return ValuationService.pctOrNull(
                v.totalDayChange(), v.totalMarketValue().subtract(v.totalDayChange()));
    }
}
