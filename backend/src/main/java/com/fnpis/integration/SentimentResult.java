package com.fnpis.integration;

import com.fnpis.domain.SentimentLabel;
import java.math.BigDecimal;

/**
 * What a sentiment engine made of one headline — not an {@code @Entity}.
 *
 * <p>Returned by {@link SentimentEngine#analyze}. The service layer converts
 * this into a {@link com.fnpis.domain.SentimentScore} before persisting, the
 * same split {@link QuoteSnapshot} and {@link DailyBar} use.
 *
 * <p>Deliberately carries no {@code articleId} and no {@code analyzedAt}: the
 * engine sees a single sentence, so it does not know which story it is looking
 * at and has no business deciding when the verdict was written.
 *
 * @param label  direction, never null - NEUTRAL is the engine's fallback
 * @param score  strength in [-1, 1], sign agreeing with {@code label}
 * @param confidence engine self-assessment in [0, 1]
 */
public record SentimentResult(
        SentimentLabel label,
        BigDecimal score,
        BigDecimal confidence) {
}
