package com.fnpis.service;

import com.fnpis.domain.Alignment;
import com.fnpis.domain.Direction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Turns a sentiment verdict plus a price move into an impact assessment
 * (requirements 5.3, E2-E4). The project's only original logic.
 *
 * <p><b>Pure function.</b> No repository, no configuration, no clock - every
 * input arrives in {@link ImpactInput}. That is deliberate: SC-012 requires the
 * three worked examples from requirements 5.3 to exist as automated tests, and
 * keeping this class free of collaborators means those tests need no database
 * and no Spring context. Injecting a repository here would turn them into
 * integration tests, which is the one thing that must not happen to the
 * project's core logic.
 *
 * <p>It answers two questions separately and never blends them: what the news
 * implied ({@link Direction}, from sentiment alone) and whether the price agreed
 * ({@link Alignment}). The disagreement is the informative case.
 */
@Component
public class ImpactEngine {

    /**
     * Scale for ratio outputs, matching {@code DECIMAL(10,6)} on
     * {@code impact_assessment}. Rounding here rather than at the column keeps
     * what the tests assert identical to what is stored.
     */
    private static final int RATIO_SCALE = 6;

    /** Scale for money, matching {@code DECIMAL(18,4)} on {@code value_impact}. */
    private static final int MONEY_SCALE = 4;

    /**
     * Assesses one story against one holding.
     *
     * @throws NullPointerException if any input other than
     *         {@link ImpactInput#priceChangeRatio()} is null - a missing
     *         sentiment or weight is a bug upstream, not a case to degrade
     */
    public ImpactOutput assess(ImpactInput input) {
        Objects.requireNonNull(input, "input");
        BigDecimal sentiment = Objects.requireNonNull(input.sentimentScore(), "sentimentScore");
        BigDecimal confidence = Objects.requireNonNull(input.confidence(), "confidence");
        BigDecimal weight = Objects.requireNonNull(input.holdingWeight(), "holdingWeight");
        BigDecimal holdingValue = Objects.requireNonNull(input.holdingValue(), "holdingValue");
        BigDecimal epsilon = Objects.requireNonNull(input.epsilon(), "epsilon");
        BigDecimal ratio = input.priceChangeRatio();

        BigDecimal expectedImpact = sentiment.multiply(confidence)
                .multiply(weight)
                .setScale(RATIO_SCALE, RoundingMode.HALF_UP);

        Direction direction = directionOf(sentiment);

        // No return means no observed side of the story: the three price-derived
        // outputs stay null together and the verdict is "cannot tell" rather than
        // a fabricated zero (EC-18, EC-19).
        if (ratio == null) {
            return new ImpactOutput(expectedImpact, null, null, direction, Alignment.INCONCLUSIVE);
        }

        BigDecimal observedContribution = weight.multiply(ratio)
                .setScale(RATIO_SCALE, RoundingMode.HALF_UP);
        BigDecimal valueImpact = holdingValue.multiply(ratio)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return new ImpactOutput(
                expectedImpact,
                observedContribution,
                valueImpact,
                direction,
                alignmentOf(sentiment, ratio, epsilon));
    }

    /**
     * Direction comes from sentiment only (E2). What the price did is reported
     * separately by {@link Alignment}.
     */
    private Direction directionOf(BigDecimal sentiment) {
        int sign = sentiment.signum();
        if (sign > 0) {
            return Direction.POSITIVE;
        }
        if (sign < 0) {
            return Direction.NEGATIVE;
        }
        return Direction.NEUTRAL;
    }

    private Alignment alignmentOf(BigDecimal sentiment, BigDecimal ratio, BigDecimal epsilon) {
        // Strictly less than epsilon, per requirements 5.3: a move of exactly
        // epsilon counts as a reaction. compareTo, never equals - BigDecimal
        // equals is scale-sensitive, so 0.005 and 0.0050 would not match.
        if (ratio.abs().compareTo(epsilon) < 0) {
            return Alignment.INCONCLUSIVE;
        }

        int sentimentSign = sentiment.signum();
        // Neutral sentiment predicted no direction, so there is nothing for the
        // price to confirm or contradict.
        if (sentimentSign == 0) {
            return Alignment.INCONCLUSIVE;
        }

        return sentimentSign == ratio.signum() ? Alignment.CONFIRMED : Alignment.DIVERGENT;
    }
}