package com.fnpis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.fnpis.domain.Alignment;
import com.fnpis.domain.Direction;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The acceptance tests for the impact engine.
 *
 * <p>The three worked examples from requirements 5.3 are SC-012: they must exist
 * as automated tests and pass. They run with no database and no Spring context
 * because {@link ImpactEngine} is a pure function - that is the whole reason it
 * is one.
 *
 * <p>Expected values are asserted at full stored precision, not at the rounded
 * figures the requirements document prints. Requirements 5.3 writes example 1's
 * expected impact as +0.192; the stored value is 0.191981, and 0.192 is what the
 * display layer rounds it to.
 */
class ImpactEngineTest {

    private static final BigDecimal EPSILON = new BigDecimal("0.005");

    private final ImpactEngine engine = new ImpactEngine();

    @Nested
    @DisplayName("the three worked examples from requirements 5.3 (SC-012)")
    class WorkedExamples {

        @Test
        @DisplayName("example 1: positive news, price rose - CONFIRMED")
        void example1Confirmed() {
            // "Nvidia beats Q2 estimates" - s=0.72, c=0.88, w=0.303, r=+4.15%,
            // NVDA holding worth $38,900.25 of a $128,450.75 portfolio.
            ImpactOutput result = engine.assess(new ImpactInput(
                    new BigDecimal("0.72"),
                    new BigDecimal("0.88"),
                    new BigDecimal("0.303"),
                    new BigDecimal("0.0415"),
                    new BigDecimal("38900.25"),
                    EPSILON));

            assertThat(result.expectedImpact()).isEqualByComparingTo("0.191981");
            assertThat(result.observedContribution()).isEqualByComparingTo("0.012575");
            assertThat(result.valueImpact()).isEqualByComparingTo("1614.3604");
            assertThat(result.direction()).isEqualTo(Direction.POSITIVE);
            assertThat(result.alignment()).isEqualTo(Alignment.CONFIRMED);
        }

        @Test
        @DisplayName("example 2: positive news, price fell - DIVERGENT")
        void example2Divergent() {
            // "Tesla announces new factory" - s=0.55, c=0.70, w=0.12, r=-2.30%.
            // Requirements 5.3 gives no holding value for this example, so one is
            // chosen here: 12% of the $128,450.75 portfolio used throughout 5.3.
            ImpactOutput result = engine.assess(new ImpactInput(
                    new BigDecimal("0.55"),
                    new BigDecimal("0.70"),
                    new BigDecimal("0.12"),
                    new BigDecimal("-0.023"),
                    new BigDecimal("15414.09"),
                    EPSILON));

            assertThat(result.expectedImpact()).isEqualByComparingTo("0.046200");
            assertThat(result.observedContribution()).isEqualByComparingTo("-0.002760");
            assertThat(result.valueImpact()).isEqualByComparingTo("-354.5241");

            // The point of this example: sentiment still says POSITIVE. The
            // engine must not rewrite the prediction to match the outcome.
            assertThat(result.direction()).isEqualTo(Direction.POSITIVE);
            assertThat(result.alignment()).isEqualTo(Alignment.DIVERGENT);
        }

        @Test
        @DisplayName("example 3: price barely moved - INCONCLUSIVE")
        void example3Inconclusive() {
            // "Apple updates App Store policy" - s=0.20, c=0.40, w=0.328,
            // r=+0.18%, which is inside epsilon.
            ImpactOutput result = engine.assess(new ImpactInput(
                    new BigDecimal("0.20"),
                    new BigDecimal("0.40"),
                    new BigDecimal("0.328"),
                    new BigDecimal("0.0018"),
                    new BigDecimal("42131.85"),
                    EPSILON));

            assertThat(result.expectedImpact()).isEqualByComparingTo("0.026240");
            assertThat(result.alignment()).isEqualTo(Alignment.INCONCLUSIVE);

            // INCONCLUSIVE because the move was noise, not because data was
            // missing - so the observed figures are still real and still reported.
            assertThat(result.observedContribution()).isEqualByComparingTo("0.000590");
            assertThat(result.valueImpact()).isEqualByComparingTo("75.8373");
            assertThat(result.direction()).isEqualTo(Direction.POSITIVE);
        }
    }

    @Nested
    @DisplayName("epsilon boundary")
    class EpsilonBoundary {

        @Test
        @DisplayName("a move of exactly epsilon counts as a reaction, not noise")
        void exactlyEpsilonIsAVerdict() {
            // Requirements 5.3 says |r| < epsilon is INCONCLUSIVE, so the
            // boundary itself is a verdict. Flipping this to <= would silently
            // discard the edge case and under-report the agreement rate.
            ImpactOutput result = engine.assess(input(new BigDecimal("0.005")));

            assertThat(result.alignment()).isEqualTo(Alignment.CONFIRMED);
        }

        @Test
        @DisplayName("just inside epsilon is noise")
        void justInsideEpsilonIsNoise() {
            assertThat(engine.assess(input(new BigDecimal("0.004999"))).alignment())
                    .isEqualTo(Alignment.INCONCLUSIVE);
        }

        @Test
        @DisplayName("just outside epsilon is a verdict")
        void justOutsideEpsilonIsAVerdict() {
            assertThat(engine.assess(input(new BigDecimal("0.005001"))).alignment())
                    .isEqualTo(Alignment.CONFIRMED);
        }

        @Test
        @DisplayName("epsilon compares on value, not scale")
        void epsilonComparesOnValue() {
            // 0.0050 equals 0.005 numerically but not by BigDecimal.equals.
            // Using equals anywhere in the comparison chain breaks this case.
            assertThat(engine.assess(input(new BigDecimal("0.0050"))).alignment())
                    .isEqualTo(Alignment.CONFIRMED);
        }

        @Test
        @DisplayName("a large negative move against positive news is DIVERGENT, not noise")
        void negativeMovesUseAbsoluteValue() {
            // Guards against comparing r to epsilon without abs(), which would
            // treat every fall as noise and make DIVERGENT unreachable.
            assertThat(engine.assess(input(new BigDecimal("-0.08"))).alignment())
                    .isEqualTo(Alignment.DIVERGENT);
        }

        private ImpactInput input(BigDecimal ratio) {
            return new ImpactInput(
                    new BigDecimal("0.60"),
                    new BigDecimal("0.90"),
                    new BigDecimal("0.25"),
                    ratio,
                    new BigDecimal("10000.00"),
                    EPSILON);
        }
    }

    @Nested
    @DisplayName("missing price data (EC-18, EC-19)")
    class MissingPriceData {

        @Test
        @DisplayName("a null return yields INCONCLUSIVE with all price figures null")
        void nullRatioIsInconclusive() {
            // EC-18: no previous close. A zero here would read as "the price did
            // not move", which is a different claim from "we do not know".
            ImpactOutput result = engine.assess(new ImpactInput(
                    new BigDecimal("0.72"),
                    new BigDecimal("0.88"),
                    new BigDecimal("0.303"),
                    null,
                    new BigDecimal("38900.25"),
                    EPSILON));

            assertThat(result.alignment()).isEqualTo(Alignment.INCONCLUSIVE);
            assertThat(result.observedContribution()).isNull();
            assertThat(result.valueImpact()).isNull();
        }

        @Test
        @DisplayName("expected impact survives a null return - it needs no price")
        void expectedImpactStillComputedWithoutPrice() {
            // The sentiment side of the story is still worth reporting: the news
            // predicted something even though the market cannot be checked.
            ImpactOutput result = engine.assess(new ImpactInput(
                    new BigDecimal("0.72"),
                    new BigDecimal("0.88"),
                    new BigDecimal("0.303"),
                    null,
                    new BigDecimal("38900.25"),
                    EPSILON));

            assertThat(result.expectedImpact()).isEqualByComparingTo("0.191981");
            assertThat(result.direction()).isEqualTo(Direction.POSITIVE);
        }
    }

    @Nested
    @DisplayName("zero weight (EC-22)")
    class ZeroWeight {

        @Test
        @DisplayName("zero weight yields zero expected impact without dividing")
        void zeroWeightIsSafe() {
            // EC-22: the caller sets w=0 when the portfolio total is zero rather
            // than dividing by it. The engine must accept that without blowing up.
            ImpactOutput result = engine.assess(new ImpactInput(
                    new BigDecimal("0.72"),
                    new BigDecimal("0.88"),
                    BigDecimal.ZERO,
                    new BigDecimal("0.0415"),
                    BigDecimal.ZERO,
                    EPSILON));

            assertThat(result.expectedImpact()).isEqualByComparingTo("0");
            assertThat(result.observedContribution()).isEqualByComparingTo("0");
            assertThat(result.valueImpact()).isEqualByComparingTo("0");

            // The price still moved, so the direction check is still meaningful.
            assertThat(result.alignment()).isEqualTo(Alignment.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("direction comes from sentiment alone (E2)")
    class DirectionFromSentiment {

        @Test
        @DisplayName("negative sentiment with a falling price is CONFIRMED")
        void negativeSentimentFallingPrice() {
            ImpactOutput result = engine.assess(new ImpactInput(
                    new BigDecimal("-0.55"),
                    new BigDecimal("0.80"),
                    new BigDecimal("0.20"),
                    new BigDecimal("-0.03"),
                    new BigDecimal("20000.00"),
                    EPSILON));

            assertThat(result.direction()).isEqualTo(Direction.NEGATIVE);
            assertThat(result.alignment()).isEqualTo(Alignment.CONFIRMED);
            assertThat(result.expectedImpact()).isEqualByComparingTo("-0.088000");
        }

        @Test
        @DisplayName("negative sentiment with a rising price is DIVERGENT")
        void negativeSentimentRisingPrice() {
            ImpactOutput result = engine.assess(new ImpactInput(
                    new BigDecimal("-0.55"),
                    new BigDecimal("0.80"),
                    new BigDecimal("0.20"),
                    new BigDecimal("0.03"),
                    new BigDecimal("20000.00"),
                    EPSILON));

            assertThat(result.direction()).isEqualTo(Direction.NEGATIVE);
            assertThat(result.alignment()).isEqualTo(Alignment.DIVERGENT);
        }

        @Test
        @DisplayName("a zero score is NEUTRAL even when written with trailing zeros")
        void zeroScoreIsNeutral() {
            // 0.00 is not equal to 0 by BigDecimal.equals, so signum/compareTo is
            // the only safe way to classify this (entity docs warn about it).
            ImpactOutput result = engine.assess(new ImpactInput(
                    new BigDecimal("0.00"),
                    new BigDecimal("0.50"),
                    new BigDecimal("0.20"),
                    new BigDecimal("0.04"),
                    new BigDecimal("20000.00"),
                    EPSILON));

            assertThat(result.direction()).isEqualTo(Direction.NEUTRAL);
        }

        @Test
        @DisplayName("neutral sentiment predicts nothing, so nothing can confirm it")
        void neutralSentimentIsInconclusive() {
            // The price moved well past epsilon, but a NEUTRAL prediction has no
            // direction to agree or disagree with. Calling this CONFIRMED would
            // inflate the agreement rate that E5 reports.
            ImpactOutput result = engine.assess(new ImpactInput(
                    BigDecimal.ZERO,
                    new BigDecimal("0.50"),
                    new BigDecimal("0.20"),
                    new BigDecimal("0.04"),
                    new BigDecimal("20000.00"),
                    EPSILON));

            assertThat(result.alignment()).isEqualTo(Alignment.INCONCLUSIVE);
        }
    }

    @Nested
    @DisplayName("required inputs")
    class RequiredInputs {

        @Test
        @DisplayName("a missing sentiment score is a bug upstream, not a degraded case")
        void nullSentimentRejected() {
            ImpactInput input = new ImpactInput(
                    null,
                    new BigDecimal("0.88"),
                    new BigDecimal("0.303"),
                    new BigDecimal("0.0415"),
                    new BigDecimal("38900.25"),
                    EPSILON);

            assertThatNullPointerException().isThrownBy(() -> engine.assess(input));
        }
    }
}