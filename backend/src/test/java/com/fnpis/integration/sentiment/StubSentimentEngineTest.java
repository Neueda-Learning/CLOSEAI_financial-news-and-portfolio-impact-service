package com.fnpis.integration.sentiment;

import static org.assertj.core.api.Assertions.assertThat;

import com.fnpis.domain.SentimentLabel;
import com.fnpis.integration.SentimentResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The stub engine's contract: deterministic, never null, always in range.
 *
 * <p>Its output feeds {@link com.fnpis.service.SentimentResultValidator}, so the
 * range assertions here are what lets that validator stay a guard against the
 * real LLM rather than a filter the stub keeps tripping.
 */
class StubSentimentEngineTest {

    private final StubSentimentEngine engine = new StubSentimentEngine();

    @Nested
    @DisplayName("scoring by keyword")
    class Scoring {

        @Test
        @DisplayName("a headline with positive terms scores positive")
        void positiveHeadline() {
            SentimentResult result = engine.analyze("Nvidia beats Q2 estimates, raises guidance");

            assertThat(result.label()).isEqualTo(SentimentLabel.POSITIVE);
            // "beats" and "raises" - two hits at 0.25 each.
            assertThat(result.score()).isEqualByComparingTo("0.50");
            assertThat(result.confidence()).isEqualByComparingTo("0.60");
        }

        @Test
        @DisplayName("a headline with negative terms scores negative")
        void negativeHeadline() {
            SentimentResult result = engine.analyze("Tesla recalls 12,000 vehicles over software fault");

            assertThat(result.label()).isEqualTo(SentimentLabel.NEGATIVE);
            assertThat(result.score()).isEqualByComparingTo("-0.25");
        }

        @Test
        @DisplayName("an unremarkable headline is NEUTRAL with low confidence")
        void neutralHeadline() {
            SentimentResult result = engine.analyze("Apple updates App Store review policy");

            assertThat(result.label()).isEqualTo(SentimentLabel.NEUTRAL);
            assertThat(result.score()).isEqualByComparingTo("0");
            assertThat(result.confidence()).isEqualByComparingTo("0.30");
        }

        @Test
        @DisplayName("terms on both sides cancelling out gives NEUTRAL, not a false verdict")
        void mixedHeadlineCancelsOut() {
            SentimentResult result = engine.analyze("Chipmaker beats estimates but warns on guidance");

            assertThat(result.label()).isEqualTo(SentimentLabel.NEUTRAL);
            assertThat(result.score()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("matching is case-insensitive")
        void caseInsensitive() {
            assertThat(engine.analyze("NVIDIA BEATS ESTIMATES").label())
                    .isEqualTo(SentimentLabel.POSITIVE);
        }

        @Test
        @DisplayName("many hits clamp the score to 1 rather than exceeding the schema range")
        void scoreIsClamped() {
            SentimentResult result = engine.analyze(
                    "Record profit, growth, upgrade, approval and a rally as the stock surges");

            assertThat(result.score()).isEqualByComparingTo("1");
            assertThat(result.label()).isEqualTo(SentimentLabel.POSITIVE);
        }
    }

    @Nested
    @DisplayName("word boundaries")
    class WordBoundaries {

        @Test
        @DisplayName("'win' does not match inside 'winding'")
        void noSubstringMatch() {
            assertThat(engine.analyze("Company begins winding down its European unit").label())
                    .isEqualTo(SentimentLabel.NEUTRAL);
        }

        @Test
        @DisplayName("a term at the very start of the headline still matches")
        void matchAtStart() {
            assertThat(engine.analyze("Losses widen at the regional carrier").label())
                    .isEqualTo(SentimentLabel.NEGATIVE);
        }

        @Test
        @DisplayName("a term followed by punctuation still matches")
        void matchBeforePunctuation() {
            assertThat(engine.analyze("Quarterly profit, up sharply").label())
                    .isEqualTo(SentimentLabel.POSITIVE);
        }
    }

    @Nested
    @DisplayName("contract guarantees")
    class Contract {

        @Test
        @DisplayName("null and blank headlines return NEUTRAL rather than throwing")
        void nullAndBlankAreNeutral() {
            assertThat(engine.analyze(null).label()).isEqualTo(SentimentLabel.NEUTRAL);
            assertThat(engine.analyze("").label()).isEqualTo(SentimentLabel.NEUTRAL);
            assertThat(engine.analyze("   ").label()).isEqualTo(SentimentLabel.NEUTRAL);
        }

        @Test
        @DisplayName("the same headline always gives the same verdict")
        void deterministic() {
            String headline = "Tesla recalls 12,000 vehicles over software fault";

            assertThat(engine.analyze(headline)).isEqualTo(engine.analyze(headline));
        }

        @Test
        @DisplayName("score stays in [-1, 1] and confidence in [0, 1] for every case above")
        void outputsAreAlwaysInRange() {
            String[] headlines = {
                "Nvidia beats Q2 estimates, raises guidance",
                "Tesla recalls 12,000 vehicles over software fault",
                "Apple updates App Store review policy",
                "Record profit, growth, upgrade, approval and a rally as the stock surges",
                "Fraud probe, lawsuit, layoffs, losses and a downgrade as shares plunge",
                "",
            };

            for (String headline : headlines) {
                SentimentResult result = engine.analyze(headline);
                assertThat(result.label()).isNotNull();
                assertThat(result.score())
                        .isBetween(new BigDecimal("-1"), BigDecimal.ONE);
                assertThat(result.confidence())
                        .isBetween(BigDecimal.ZERO, BigDecimal.ONE);
            }
        }

        @Test
        @DisplayName("the label always agrees with the score, so the validator never corrects it")
        void labelAgreesWithScore() {
            String[] headlines = {
                "Nvidia beats Q2 estimates, raises guidance",
                "Tesla recalls 12,000 vehicles over software fault",
                "Apple updates App Store review policy",
                "Chipmaker beats estimates but warns on guidance",
            };

            for (String headline : headlines) {
                SentimentResult result = engine.analyze(headline);
                assertThat(result.label().isConsistentWith(result.score())).isTrue();
            }
        }

        @Test
        @DisplayName("modelVersion identifies the engine and its word lists")
        void modelVersionIsStamped() {
            assertThat(engine.modelVersion()).isEqualTo("stub-v1");
        }
    }
}
