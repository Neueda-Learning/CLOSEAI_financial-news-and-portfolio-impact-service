package com.fnpis.service;

import com.fnpis.domain.SentimentLabel;
import com.fnpis.integration.SentimentResult;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Gate between an engine's answer and the database (5.2, decision 5).
 *
 * <p>This is the real defence against prompt injection. Fencing the headline
 * inside a data boundary in the prompt helps, but a model can always be talked
 * into ignoring it; refusing to store anything that does not fit the schema
 * cannot be talked out of. Every verdict passes through here, stub included -
 * the stub is trusted today, and a validator that only runs on the untrusted
 * path is one refactor away from not running at all.
 *
 * <p>Four rules, and the fourth behaves differently from the others:
 *
 * <table border="1">
 * <caption>Validation rules</caption>
 * <tr><th>Rule</th><th>On breach</th></tr>
 * <tr><td>{@code label} present and a known value</td><td>discard</td></tr>
 * <tr><td>{@code score} in [-1, 1]</td><td>discard</td></tr>
 * <tr><td>{@code confidence} in [0, 1]</td><td>discard</td></tr>
 * <tr><td>{@code score} sign agrees with {@code label}</td>
 *     <td><b>correct the label, keep the row</b></td></tr>
 * </table>
 *
 * <p>Rule four corrects because the number is more trustworthy than the word:
 * a model that answers POSITIVE with {@code score = -0.3} has picked the label
 * carelessly, not miscalculated, and discarding would throw away a usable
 * verdict along with the LLM call that paid for it. Rules one to three have
 * nothing to fall back on, so they discard.
 *
 * <p>An unknown label string never reaches this class - Jackson fails to bind
 * it and {@code label} arrives null, which rule one catches either way.
 */
@Component
public class SentimentResultValidator {

    private static final BigDecimal SCORE_MIN = BigDecimal.ONE.negate();
    private static final BigDecimal SCORE_MAX = BigDecimal.ONE;
    private static final BigDecimal CONFIDENCE_MIN = BigDecimal.ZERO;
    private static final BigDecimal CONFIDENCE_MAX = BigDecimal.ONE;

    /**
     * Validates one verdict, correcting the label where rule four applies.
     *
     * @param result what the engine returned, may be null
     * @return the verdict to store, or empty when it must be discarded
     */
    public Optional<SentimentResult> validate(SentimentResult result) {
        if (result == null
                || result.label() == null
                || !inRange(result.score(), SCORE_MIN, SCORE_MAX)
                || !inRange(result.confidence(), CONFIDENCE_MIN, CONFIDENCE_MAX)) {
            return Optional.empty();
        }

        if (result.label().isConsistentWith(result.score())) {
            return Optional.of(result);
        }

        // Rule four: trust the number, rewrite the word.
        return Optional.of(new SentimentResult(
                labelFor(result.score()),
                result.score(),
                result.confidence()));
    }

    /**
     * The label a score implies. {@code signum} rather than {@code equals},
     * because {@code BigDecimal.equals} is scale-sensitive and would read
     * {@code 0.00} as something other than zero (entity guide 5).
     */
    private SentimentLabel labelFor(BigDecimal score) {
        int sign = score.signum();
        if (sign > 0) {
            return SentimentLabel.POSITIVE;
        }
        if (sign < 0) {
            return SentimentLabel.NEGATIVE;
        }
        return SentimentLabel.NEUTRAL;
    }

    /** Inclusive on both ends, via {@code compareTo} for the reason above. */
    private boolean inRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value != null
                && value.compareTo(min) >= 0
                && value.compareTo(max) <= 0;
    }
}
