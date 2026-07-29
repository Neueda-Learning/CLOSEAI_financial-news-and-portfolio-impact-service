package com.fnpis.service;

import java.math.BigDecimal;

/**
 * One position's contribution to its portfolio, for the impact engine (module E).
 *
 * <p>Two of the four inputs in requirements 5.3 come from here - {@code w} and
 * the market value {@code valueImpact} multiplies. The other two, sentiment and
 * price change, come from modules D and B.
 *
 * <p><b>Weight is BigDecimal here, not the Double the API exposes.</b> On the
 * wire a weight is only displayed, so a JSON number is right (contract 1.2). In
 * module E it gets multiplied into money - {@code expectedImpact = s × c × w},
 * {@code observedContrib = w × r} - and architecture 6.2 keeps money off
 * double end to end. Handing E a Double would make it convert back and inherit
 * the rounding this type exists to avoid.
 *
 * @param marketValue null when the symbol has no quote (EC-13); module E cannot
 *                    assess a position it cannot value, and a zero here would
 *                    read as "no impact" rather than "unknown"
 * @param weight      null for the same reason, at 6 decimals to match the
 *                    DECIMAL(10,6) impact columns
 */
public record PositionWeight(
        String symbol,
        BigDecimal quantity,
        BigDecimal marketValue,
        BigDecimal weight) {

    /** True when this position can take part in an impact assessment. */
    public boolean assessable() {
        return marketValue != null && weight != null;
    }
}
