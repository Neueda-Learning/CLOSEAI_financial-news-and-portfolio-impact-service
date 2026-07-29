package com.fnpis.service;

import com.fnpis.api.internal.dto.PriceQuoteDTO;
import com.fnpis.api.internal.dto.ValuationHistoryResponse;
import com.fnpis.common.Freshness;
import com.fnpis.domain.PortfolioValuationSnapshot;
import com.fnpis.domain.PriceQuote;
import com.fnpis.repository.PortfolioRepository;
import com.fnpis.repository.PortfolioValuationSnapshotRepository;
import com.fnpis.repository.PriceQuoteRepository;
import com.fnpis.repository.SecurityRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Read-only price data for the API layer (B1, B2, F5).
 *
 * <p>Never calls a provider — serves whatever the refresh jobs have landed
 * (architecture decision 2).
 */
@Service
public class PriceReadService {

    private static final Logger log = LoggerFactory.getLogger(PriceReadService.class);

    private final PriceQuoteRepository quoteRepo;
    private final PortfolioValuationSnapshotRepository snapshotRepo;
    private final PortfolioRepository portfolioRepo;
    private final SecurityRepository securityRepo;
    private final Duration freshnessBudget;
    private static final int PCT_SCALE = 2;

    public PriceReadService(
            PriceQuoteRepository quoteRepo,
            PortfolioValuationSnapshotRepository snapshotRepo,
            PortfolioRepository portfolioRepo,
            SecurityRepository securityRepo,
            @Value("${app.valuation.quote-freshness-budget-seconds}") int freshnessSeconds) {
        this.quoteRepo = quoteRepo;
        this.snapshotRepo = snapshotRepo;
        this.portfolioRepo = portfolioRepo;
        this.securityRepo = securityRepo;
        this.freshnessBudget = Duration.ofSeconds(freshnessSeconds);
    }

    /**
     * Latest quote for one symbol (B1).
     *
     * @return the quote DTO, or null when the symbol is not in the {@code security} table
     *         (controller maps that to 404). Known symbols without a quote yet get
     *         {@code quoteAvailable: false} so the frontend can show "no data" (EC-13).
     */
    public PriceQuoteDTO quote(String symbol, Instant now) {
        String s = symbol.toUpperCase();
        if (!securityRepo.existsById(s)) {
            return null;
        }
        return quoteRepo.findById(s)
                .map(q -> toDto(q, now))
                .orElseGet(() -> new PriceQuoteDTO(
                        s, null, null, null, null, false, null, true));
    }

    /**
     * Portfolio value over time (B2, F5).
     *
     * <p>Returns an empty list on a fresh install — the frontend must render
     * "collecting data" (contract §4).
     */
    public ValuationHistoryResponse valuationHistory(Long portfolioId, LocalDate from, LocalDate to) {
        if (!portfolioRepo.existsById(portfolioId)) {
            return null;
        }
        List<PortfolioValuationSnapshot> snapshots =
                snapshotRepo.findByPortfolioIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
                        portfolioId, from, to);
        if (snapshots.isEmpty()) {
            return new ValuationHistoryResponse(portfolioId, Collections.emptyList(), null, true);
        }
        var points = snapshots.stream()
                .map(s -> new ValuationHistoryResponse.ValuationPoint(
                        s.getSnapshotDate().toString(), s.getTotalValue()))
                .toList();
        Instant latest = snapshots.get(snapshots.size() - 1).getCreatedAt();
        return new ValuationHistoryResponse(portfolioId, points, latest, false);
    }

    private PriceQuoteDTO toDto(PriceQuote q, Instant now) {
        Freshness f = Freshness.of(q.getAsOf(), freshnessBudget, now);
        BigDecimal change = q.change();
        BigDecimal changePct = null;
        if (change != null && q.getPreviousClose() != null
                && q.getPreviousClose().compareTo(BigDecimal.ZERO) != 0) {
            changePct = change.divide(q.getPreviousClose(), 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(PCT_SCALE, RoundingMode.HALF_UP);
        }
        return new PriceQuoteDTO(
                q.getSymbol(), q.getPrice(), q.getPreviousClose(),
                change, changePct, true, f.asOf(), f.stale());
    }
}
