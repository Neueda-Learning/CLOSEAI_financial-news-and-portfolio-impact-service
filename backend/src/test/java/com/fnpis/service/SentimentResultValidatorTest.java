package com.fnpis.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fnpis.domain.SentimentLabel;
import com.fnpis.integration.SentimentResult;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The four validation rules on an engine's answer (5.2, decision 5).
 *
 * <p>CLAUDE.md's test table names these cases specifically: illegal label, score
 * out of range, non-JSON, and a score that disagrees with its label. The
 * non-JSON case is covered here as a null result, because a body that will not
 * parse never becomes a {@link SentimentResult} - the engine catches the parse
 * failure and what reaches the validator is nothing at all.
 */
class SentimentResultValidatorTest {

    private final SentimentResultValidator validator = new SentimentResultValidator();

    @Nested
    @DisplayName("rules 1-3: discard, because there is nothing to fall back on")
    class Discards {

        @Test
        @DisplayName("a null result is discarded - this is the non-JSON case")
        void nullResultDiscarded() {
            assertThat(validator.validate(null)).isEmpty();
        }

        @Test
        @DisplayName("a missing label is discarded - an unknown label string arrives as null")
        void nullLabelDiscarded() {
            assertThat(validator.validate(new SentimentResult(
                    null, new BigDecimal("0.5"), new BigDecimal("0.8")))).isEmpty();
        }

        @Test
        @DisplayName("a score above 1 is discarded")
        void scoreAboveRangeDiscarded() {
            assertThat(validator.validate(new SentimentResult(
                    SentimentLabel.POSITIVE, new BigDecimal("1.4"), new BigDecimal("0.8")))).isEmpty();
        }

        @Test
        @DisplayName("a score below -1 is discarded")
        void scoreBelowRangeDiscarded() {
            assertThat(validator.validate(new SentimentResult(
                    SentimentLabel.NEGATIVE, new BigDecimal("-1.01"), new BigDecimal("0.8")))).isEmpty();
        }

        @Test
        @DisplayName("a null score is discarded")
        void nullScoreDiscarded() {
            assertThat(validator.validate(new SentimentResult(
                    SentimentLabel.POSITIVE, null, new BigDecimal("0.8")))).isEmpty();
        }

        @Test
        @DisplayName("a negative confidence is discarded")
        void confidenceBelowRangeDiscarded() {
            assertThat(validator.validate(new SentimentResult(
                    SentimentLabel.POSITIVE, new BigDecimal("0.5"), new BigDecimal("-0.1")))).isEmpty();
        }

        @Test
        @DisplayName("a confidence above 1 is discarded")
        void confidenceAboveRangeDiscarded() {
            assertThat(validator.validate(new SentimentResult(
                    SentimentLabel.POSITIVE, new BigDecimal("0.5"), new BigDecimal("1.2")))).isEmpty();
        }

        @Test
        @DisplayName("a null confidence is discarded")
        void nullConfidenceDiscarded() {
            assertThat(validator.validate(new SentimentResult(
                    SentimentLabel.POSITIVE, new BigDecimal("0.5"), null))).isEmpty();
        }
    }

    @Nested
    @DisplayName("range bounds are inclusive, compared by value not by scale")
    class Bounds {

        @Test
        @DisplayName("score of exactly 1 is accepted")
        void scoreAtUpperBoundAccepted() {
            assertThat(validator.validate(new SentimentResult(
                    SentimentLabel.POSITIVE, BigDecimal.ONE, new BigDecimal("0.8")))).isPresent();
        }

        @Test
        @DisplayName("score of exactly -1 is accepted")
        void scoreAtLowerBoundAccepted() {
            assertThat(validator.validate(new SentimentResult(
                    SentimentLabel.NEGATIVE, new BigDecimal("-1"), new BigDecimal("0.8")))).isPresent();
        }

        @Test
        @DisplayName("confidence of exactly 0 and exactly 1 are both accepted")
        void confidenceBoundsAccepted() {
            assertThat(validator.validate(new SentimentResult(
                    SentimentLabel.NEUTRAL, BigDecimal.ZERO, BigDecimal.ZERO))).isPresent();
            assertThat(validator.validate(new SentimentResult(
                    SentimentLabel.NEUTRAL, BigDecimal.ZERO, BigDecimal.ONE))).isPresent();
        }

        @Test
        @DisplayName("1.0000 passes the upper bound - equals would have rejected it")
        void trailingZeroesDoNotBreakTheBound() {
            assertThat(validator.validate(new SentimentResult(
                    SentimentLabel.POSITIVE, new BigDecimal("1.0000"), new BigDecimal("1.000"))))
                    .isPresent();
        }
    }

    @Nested
    @DisplayName("rule 4: a label that disagrees with its score is corrected, not discarded")
    class LabelCorrection {

        @Test
        @DisplayName("POSITIVE with score -0.3 becomes NEGATIVE and is kept")
        void positiveLabelWithNegativeScoreCorrected() {
            Optional<SentimentResult> corrected = validator.validate(new SentimentResult(
                    SentimentLabel.POSITIVE, new BigDecimal("-0.3"), new BigDecimal("0.77")));

            assertThat(corrected).isPresent();
            assertThat(corrected.get().label()).isEqualTo(SentimentLabel.NEGATIVE);
            // Score and confidence pass through untouched - only the word was wrong.
            assertThat(corrected.get().score()).isEqualByComparingTo("-0.3");
            assertThat(corrected.get().confidence()).isEqualByComparingTo("0.77");
        }

        @Test
        @DisplayName("NEGATIVE with score +0.6 becomes POSITIVE and is kept")
        void negativeLabelWithPositiveScoreCorrected() {
            Optional<SentimentResult> corrected = validator.validate(new SentimentResult(
                    SentimentLabel.NEGATIVE, new BigDecimal("0.6"), new BigDecimal("0.5")));

            assertThat(corrected).isPresent();
            assertThat(corrected.get().label()).isEqualTo(SentimentLabel.POSITIVE);
        }

        @Test
        @DisplayName("POSITIVE with score 0 becomes NEUTRAL")
        void positiveLabelWithZeroScoreBecomesNeutral() {
            Optional<SentimentResult> corrected = validator.validate(new SentimentResult(
                    SentimentLabel.POSITIVE, BigDecimal.ZERO, new BigDecimal("0.4")));

            assertThat(corrected).isPresent();
            assertThat(corrected.get().label()).isEqualTo(SentimentLabel.NEUTRAL);
        }

        @Test
        @DisplayName("POSITIVE with score 0.0000 also becomes NEUTRAL - signum, not equals")
        void scaledZeroAlsoBecomesNeutral() {
            Optional<SentimentResult> corrected = validator.validate(new SentimentResult(
                    SentimentLabel.POSITIVE, new BigDecimal("0.0000"), new BigDecimal("0.4")));

            assertThat(corrected).isPresent();
            assertThat(corrected.get().label()).isEqualTo(SentimentLabel.NEUTRAL);
        }

        @Test
        @DisplayName("NEUTRAL is consistent with any score, so it is never rewritten")
        void neutralIsNeverRewritten() {
            // SentimentLabel.isConsistentWith treats NEUTRAL as always consistent.
            // A NEUTRAL label carrying a strong score is odd but not contradictory,
            // and rewriting it would invent an opinion the engine did not give.
            Optional<SentimentResult> result = validator.validate(new SentimentResult(
                    SentimentLabel.NEUTRAL, new BigDecimal("0.9"), new BigDecimal("0.4")));

            assertThat(result).isPresent();
            assertThat(result.get().label()).isEqualTo(SentimentLabel.NEUTRAL);
        }
    }

    @Nested
    @DisplayName("a well-formed verdict passes through unchanged")
    class HappyPath {

        @Test
        @DisplayName("the same instance is returned when nothing needed correcting")
        void consistentResultPassesThrough() {
            SentimentResult input = new SentimentResult(
                    SentimentLabel.POSITIVE, new BigDecimal("0.72"), new BigDecimal("0.88"));

            assertThat(validator.validate(input)).containsSame(input);
        }
    }
}
