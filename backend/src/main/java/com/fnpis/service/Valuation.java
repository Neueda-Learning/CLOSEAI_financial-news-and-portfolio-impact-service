package com.fnpis.service;

import com.fnpis.api.internal.dto.AllocationItem;
import com.fnpis.api.internal.dto.HoldingRow;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * A valued portfolio: per-position figures plus the totals they sum to.
 *
 * <p>The totals are exact sums of the rows, not independently rounded
 * aggregates - SC-002 has a lecturer checking them on a calculator.
 *
 * @param totalDayChange null when no holding had a previous close to compare
 * @param asOf           oldest quote behind these numbers, null when there are none
 * @param stale          any quote past its budget, or any missing (B4, SC-009)
 */
record Valuation(
        List<Priced> positions,
        BigDecimal totalMarketValue,
        BigDecimal totalCost,
        BigDecimal totalDayChange,
        Instant asOf,
        boolean stale) {

    /** Portfolio-level return, null when nothing was paid (EC-07). */
    BigDecimal unrealizedPnL() {
        return totalMarketValue.subtract(totalCost);
    }

    /**
     * Second pass: rows with weights, now that the total is known.
     *
     * <p>Weight divides by the portfolio total at full ratio scale. Dividing
     * pre-rounded values is what makes the SC-003 sum drift off 100%.
     */
    List<HoldingRow> rows() {
        return positions.stream().map(this::toRow).toList();
    }

    /** Allocation slices for the pie, skipping positions with no valuation. */
    List<AllocationItem> allocations() {
        return positions.stream()
                .filter(p -> p.marketValue() != null)
                .map(p -> new AllocationItem(
                        p.holding().getSymbol(),
                        p.marketValue(),
                        ValuationService.ratioOrNull(p.marketValue(), totalMarketValue)))
                .toList();
    }

    /**
     * Whole shares in the MVP (EC-08), so the stored 4 decimals are noise on the
     * wire - the contract shows {@code "20"}, not {@code "20.0000"}.
     */
    private static BigDecimal displayQuantity(BigDecimal quantity) {
        BigDecimal stripped = quantity.stripTrailingZeros();
        // Guards against 2E+1 for round hundreds, which stripTrailingZeros
        // produces and which no frontend expects to parse.
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }

    /**
     * Money at display precision: at least cents, more only when meaningful.
     *
     * <p>Not rounded to cents: a merged weighted average (EC-09) legitimately
     * carries 4 decimals, and rounding it here would stop {@code quantity ×
     * costBasis} from reproducing {@code totalCost}.
     */
    private static BigDecimal displayMoney(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 2 ? stripped.setScale(2) : stripped;
    }

    private HoldingRow toRow(Priced p) {
        BigDecimal pnl = p.unrealizedPnL();
        return new HoldingRow(
                p.holding().getId(),
                p.holding().getSymbol(),
                p.companyName(),
                displayQuantity(p.holding().getQuantity()),
                displayMoney(p.holding().getCostBasis()),
                // Prices are stored at 4 decimals but quoted at 2; trailing
                // zeros on the wire would not match the contract examples.
                displayMoney(p.currentPrice()),
                displayMoney(p.previousClose()),
                p.marketValue(),
                p.totalCost(),
                pnl,
                ValuationService.pctOrNull(pnl, p.totalCost()),
                p.dayChange(),
                dayChangePct(p),
                ValuationService.ratioOrNull(p.marketValue(), totalMarketValue),
                p.quoteFresh());
    }

    private Double dayChangePct(Priced p) {
        if (p.dayChange() == null || p.previousClose() == null) {
            return null;
        }
        // Per share against previous close - the quantity cancels out, so this is
        // the same number the provider would quote for the symbol itself.
        return ValuationService.pctOrNull(
                p.currentPrice().subtract(p.previousClose()), p.previousClose());
    }
}
