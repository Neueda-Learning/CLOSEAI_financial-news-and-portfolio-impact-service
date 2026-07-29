package com.fnpis.integration;

/**
 * Judges the sentiment of a single headline (D1, D2).
 *
 * <p>One engine, chosen by {@code app.providers.sentiment} ({@code agent |
 * stub}). Multi-engine comparison was dropped (decision 5), but the switch
 * stays: {@code stub} is the demo-day fallback when the LLM quota is gone
 * (architecture 8.1). Services depend on this interface and never on an
 * implementation.
 *
 * <p>Implementations analyse the headline only. Requirement AS-04 keeps article
 * bodies out of scope, so there is no overload taking one.
 */
public interface SentimentEngine {

    /**
     * Judges one headline.
     *
     * <p>The headline is untrusted third-party text. Implementations that feed
     * it to a language model must fence it inside an explicit data boundary and
     * must treat schema validation of the response as the real defence
     * ({@link com.fnpis.service.SentimentResultValidator}).
     *
     * @param headline the story's title, never null or blank
     * @return the verdict, never null - an engine that cannot decide returns
     *         NEUTRAL rather than nothing
     */
    SentimentResult analyze(String headline);

    /**
     * Model name plus prompt version, e.g. {@code stub-v1}.
     *
     * <p>Stored on every row. After a model or prompt change it is the only way
     * to tell which version produced a verdict, so it must change whenever
     * either does.
     */
    String modelVersion();
}
