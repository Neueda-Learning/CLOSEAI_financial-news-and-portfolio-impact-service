package com.fnpis.integration.sentiment;

import com.fnpis.domain.SentimentLabel;
import com.fnpis.integration.SentimentEngine;
import com.fnpis.integration.SentimentResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Keyword-scored sentiment, no network and no API key (architecture 7.5, 8.1).
 *
 * <p>Two jobs. It keeps CI green without credentials, and it is the demo-day
 * fallback if the LLM quota runs out - a fake verdict is acceptable in a way a
 * fake money figure never would be, which is why module E has no equivalent
 * stub.
 *
 * <p><b>Deterministic by design.</b> The same headline always yields the same
 * verdict, so E2E tests can assert on impact rows. That is the one thing the
 * real engine cannot promise, and it is why the stub is not merely a degraded
 * copy of it.
 *
 * <p>Active when {@code app.providers.sentiment=stub}. Note that no engine is
 * registered for {@code agent} yet, so that value currently leaves the context
 * without a {@link SentimentEngine} bean and startup fails - loudly, which is
 * the right failure. Run with {@code PROVIDER_SENTIMENT=stub}.
 */
@Component
@ConditionalOnProperty(name = "app.providers.sentiment", havingValue = "stub")
public class StubSentimentEngine implements SentimentEngine {

    /**
     * Bumped whenever the word lists or the scoring below change, because
     * {@code model_version} is the only way to tell afterwards which rows came
     * from which behaviour.
     */
    private static final String MODEL_VERSION = "stub-v1";

    private static final List<String> POSITIVE_TERMS = List.of(
            "beats", "beat", "raises", "raised", "surges", "surge", "jumps", "jump",
            "record", "upgrade", "upgraded", "growth", "profit", "wins", "win",
            "approval", "approved", "outperform", "rally", "strong", "expands");

    private static final List<String> NEGATIVE_TERMS = List.of(
            "misses", "miss", "cuts", "cut", "falls", "fall", "plunges", "plunge",
            "recall", "recalls", "lawsuit", "probe", "investigation", "downgrade",
            "downgraded", "loss", "losses", "warns", "warning", "layoffs", "fraud",
            "delay", "delays", "weak", "slumps");

    /** Score contributed by each matched term, capped at 1 by {@link #clamp}. */
    private static final BigDecimal PER_TERM = new BigDecimal("0.25");

    /**
     * Fixed confidence for a keyword match. Honest about what this engine is: it
     * counted words, so it does not get to claim near-certainty.
     */
    private static final BigDecimal MATCHED_CONFIDENCE = new BigDecimal("0.60");

    /** Confidence when nothing matched - low, and the label is NEUTRAL. */
    private static final BigDecimal UNMATCHED_CONFIDENCE = new BigDecimal("0.30");

    private static final int SCORE_SCALE = 4;

    @Override
    public SentimentResult analyze(String headline) {
        if (headline == null || headline.isBlank()) {
            return neutral();
        }

        // Lower-cased once with a fixed locale: the Turkish locale maps 'I' to a
        // dotless lower-case form, which would stop "Investigation" matching on a
        // machine configured for it.
        String text = headline.toLowerCase(Locale.ROOT);

        int hits = countHits(text, POSITIVE_TERMS) - countHits(text, NEGATIVE_TERMS);
        if (hits == 0) {
            // Either nothing matched or the two sides cancelled out. Both mean
            // this engine has no opinion, and NEUTRAL is the required answer -
            // never null (SentimentLabel's javadoc).
            return neutral();
        }

        BigDecimal score = clamp(PER_TERM.multiply(BigDecimal.valueOf(hits)))
                .setScale(SCORE_SCALE, RoundingMode.HALF_UP);
        SentimentLabel label = score.signum() > 0 ? SentimentLabel.POSITIVE : SentimentLabel.NEGATIVE;

        return new SentimentResult(label, score, MATCHED_CONFIDENCE);
    }

    @Override
    public String modelVersion() {
        return MODEL_VERSION;
    }

    private SentimentResult neutral() {
        return new SentimentResult(
                SentimentLabel.NEUTRAL,
                BigDecimal.ZERO.setScale(SCORE_SCALE, RoundingMode.UNNECESSARY),
                UNMATCHED_CONFIDENCE);
    }

    private int countHits(String text, List<String> terms) {
        int hits = 0;
        for (String term : terms) {
            if (containsWord(text, term)) {
                hits++;
            }
        }
        return hits;
    }

    /**
     * Whole-word match. Substring matching would score "recall" inside
     * "recalled" twice over and, worse, find "win" inside "winding".
     */
    private boolean containsWord(String text, String term) {
        int from = 0;
        while (true) {
            int at = text.indexOf(term, from);
            if (at < 0) {
                return false;
            }
            boolean leftClear = at == 0 || !Character.isLetterOrDigit(text.charAt(at - 1));
            int after = at + term.length();
            boolean rightClear = after == text.length()
                    || !Character.isLetterOrDigit(text.charAt(after));
            if (leftClear && rightClear) {
                return true;
            }
            from = at + 1;
        }
    }

    private BigDecimal clamp(BigDecimal score) {
        if (score.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        if (score.compareTo(BigDecimal.ONE.negate()) < 0) {
            return BigDecimal.ONE.negate();
        }
        return score;
    }
}
