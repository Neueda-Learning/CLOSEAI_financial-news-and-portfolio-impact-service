package com.fnpis.integration.sentiment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/**
 * The verdict object the model is asked to return, as it arrives on the wire -
 * before validation.
 *
 * <p>Every field is nullable and {@code label} is a raw string, not a
 * {@link com.fnpis.domain.SentimentLabel}. That is deliberate: this is untrusted
 * model output, and the point of parsing into a lenient shape first is that
 * {@link com.fnpis.service.SentimentResultValidator} gets to reject it, rather
 * than a binding failure deep in Jackson throwing before validation runs. An
 * illegal label string lands here as text and is caught by the validator's rule
 * one; {@code score}/{@code confidence} out of range are caught by rules two and
 * three.
 *
 * <p>{@code score} and {@code confidence} are {@link BigDecimal} so no precision
 * is lost between the model's number and the {@code DECIMAL} columns they end up
 * in.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SentimentJson(
        String label,
        BigDecimal score,
        BigDecimal confidence) {
}
