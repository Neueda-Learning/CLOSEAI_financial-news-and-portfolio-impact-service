package com.fnpis.service;

import java.math.BigDecimal;

/**
 * Everything {@link ImpactEngine} needs to assess one story against one holding.
 *
 * <p>Every value is passed in, including epsilon. The engine reads no
 * configuration and holds no repository, which is what lets the three worked
 * examples from requirements 5.3 run as plain unit tests with no database and no
 * Spring context (SC-012).
 *
 * @param sentimentScore s in [-1, 1] from the sentiment engine
 * @param confidence     c in [0, 1] from the sentiment engine
 * @param holdingWeight  w, this holding's value over the portfolio total; zero
 *                       when the portfolio is worth nothing (EC-22)
 * @param priceChangeRatio r as a ratio, not a percentage - 0.0415 is +4.15%.
 *                       Null when the previous close is unavailable, which
 *                       forces INCONCLUSIVE (EC-18, EC-19)
 * @param holdingValue   this holding's market value, used for the money figure
 * @param epsilon        moves smaller than this are noise, not a reaction.
 *                       A parameter rather than a field because tuning it before
 *                       the demo is an explicit requirement (5.3)
 */
public record ImpactInput(
        BigDecimal sentimentScore,
        BigDecimal confidence,
        BigDecimal holdingWeight,
        BigDecimal priceChangeRatio,
        BigDecimal holdingValue,
        BigDecimal epsilon) {
}