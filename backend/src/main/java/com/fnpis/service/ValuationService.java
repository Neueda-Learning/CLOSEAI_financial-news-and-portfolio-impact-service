package com.fnpis.service;

import com.fnpis.common.Freshness;
import com.fnpis.domain.Holding;
import com.fnpis.domain.PriceQuote;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Turns positions plus quotes into the numbers on screen (A5, B2, B3).
 *
 * <p>Deliberately has no repository dependency: everything it needs arrives as
 * arguments. This is the class SC-002 and SC-003 are checked against - a
 * lecturer will verify the totals on a calculator - so it has to be unit
 * testable without a database.
 *
 * <p>Nothing here is stored. Market value, P&amp;L and weight all derive from the
 * live quote, so a persisted copy would be wrong the moment a price refreshes.
 */
@Service
public class ValuationService {

    /**
     * Money is rounded to cents per row, then totalled from those rounded rows.
     *
     * <p>Order matters for SC-002: a lecturer adds the displayed rows on a
     * calculator and compares against the displayed total. Totalling at full
     * precision and rounding once at the end produces a total that can sit a
     * cent away from the sum of what is on screen - correct to the accountant,
     * wrong to the person checking it.
     */
    private static final int MONEY_SCALE = 2;

    /** Ratios keep 6, matching DECIMAL(10,6) on the impact columns. */
    private static final int RATIO_SCALE = 6;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final Duration quoteFreshnessBudget;

    public ValuationService(
            @Value("${app.valuation.quote-freshness-budget-seconds:300}") long budgetSeconds) {
        this.quoteFreshnessBudget = Duration.ofSeconds(budgetSeconds);
    }

    /**
     * Values every position in a portfolio.
     *
     * <p>Two passes on purpose. Market values have to be totalled before any
     * weight can be divided by that total, and rounding each weight off a
     * separately-rounded total is how the SC-003 sum drifts off 100%.
     *
     * @param holdings    positions to value, may be empty (EC-01)
     * @param quotesBySymbol latest quote per symbol; a missing entry is the
     *                       EC-13 path, not an error
     * @param namesBySymbol  company names for display
     * @param now         injected so freshness stays testable
     */
    public Valuation value(
            List<Holding> holdings,
            Map<String, PriceQuote> quotesBySymbol,
            Map<String, String> namesBySymbol,
            Instant now) {

        List<Priced> priced = new ArrayList<>(holdings.size());
        BigDecimal totalMarketValue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalDayChange = BigDecimal.ZERO;
        boolean anyDayChange = false;
        Instant oldestAsOf = null;
        boolean anyQuoteMissing = false;

        for (Holding h : holdings) {
            PriceQuote quote = quotesBySymbol.get(h.getSymbol());
            Priced p = price(h, quote, namesBySymbol.get(h.getSymbol()), now);
            priced.add(p);

            totalCost = totalCost.add(p.totalCost());
            if (p.marketValue() != null) {
                totalMarketValue = totalMarketValue.add(p.marketValue());
            }
            if (p.dayChange() != null) {
                totalDayChange = totalDayChange.add(p.dayChange());
                anyDayChange = true;
            }
            if (quote == null) {
                anyQuoteMissing = true;
            } else if (oldestAsOf == null || quote.getAsOf().isBefore(oldestAsOf)) {
                oldestAsOf = quote.getAsOf();
            }
        }

        // One stale quote makes the whole figure stale - a total is only as
        // current as its oldest input. A missing quote counts as stale too:
        // the number on screen is provably incomplete (B4, SC-009).
        //
        // An empty portfolio is the exception. Its zero total rests on no quote
        // at all, so there is nothing to be out of date; reporting stale there
        // would put a warning badge on a number that is exactly right (EC-01).
        Freshness freshness = holdings.isEmpty()
                ? Freshness.fresh(null)
                : Freshness.of(oldestAsOf, quoteFreshnessBudget, now);
        boolean stale = !holdings.isEmpty() && (freshness.stale() || anyQuoteMissing);

        return new Valuation(
                priced,
                scaleMoney(totalMarketValue),
                scaleMoney(totalCost),
                anyDayChange ? scaleMoney(totalDayChange) : null,
                freshness.asOf(),
                stale);
    }

    private Priced price(Holding h, PriceQuote quote, String companyName, Instant now) {
        BigDecimal totalCost = scaleMoney(h.totalCost());
        boolean quoteFresh = quote != null
                && !Freshness.of(quote.getAsOf(), quoteFreshnessBudget, now).stale();

        // No quote row at all: nothing honest to show as a price, so market
        // value stays null rather than falling back to cost, which would look
        // like a real valuation and quietly corrupt the total (EC-13).
        if (quote == null) {
            return new Priced(h, companyName, totalCost, null, null, null, null, false);
        }

        BigDecimal price = quote.getPrice();
        BigDecimal marketValue = scaleMoney(h.getQuantity().multiply(price));
        BigDecimal dayChange = quote.getPreviousClose() == null
                ? null
                : scaleMoney(h.getQuantity().multiply(price.subtract(quote.getPreviousClose())));

        return new Priced(h, companyName, totalCost, price, quote.getPreviousClose(),
                marketValue, dayChange, quoteFresh);
    }

    private static BigDecimal scaleMoney(BigDecimal v) {
        return v == null ? null : v.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Percentage change as a display number, or null when the base is zero.
     *
     * <p>Returning null rather than zero or Infinity is what EC-07 requires: a
     * gifted position has no meaningful return, and the frontend shows a dash.
     */
    static Double pctOrNull(BigDecimal delta, BigDecimal base) {
        if (delta == null || base == null || base.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return delta.multiply(HUNDRED)
                .divide(base, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /** Ratio of part to whole as a display number, or null when the whole is zero. */
    static Double ratioOrNull(BigDecimal part, BigDecimal whole) {
        BigDecimal exact = exactRatioOrNull(part, whole);
        return exact == null ? null : exact.doubleValue();
    }

    /**
     * The same ratio kept as BigDecimal, for callers that compute with it.
     *
     * <p>Module E multiplies a weight into money ({@code w × r}, {@code s × c ×
     * w}), and architecture 6.2 keeps money off double. The Double above is for
     * the wire, where a weight is only ever displayed.
     */
    static BigDecimal exactRatioOrNull(BigDecimal part, BigDecimal whole) {
        if (part == null || whole == null || whole.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return part.divide(whole, RATIO_SCALE, RoundingMode.HALF_UP);
    }
}
