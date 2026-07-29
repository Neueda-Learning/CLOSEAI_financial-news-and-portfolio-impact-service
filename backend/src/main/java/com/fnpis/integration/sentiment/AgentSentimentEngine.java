package com.fnpis.integration.sentiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnpis.domain.SentimentLabel;
import com.fnpis.integration.SentimentEngine;
import com.fnpis.integration.SentimentResult;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

/**
 * The LLM-backed sentiment engine (D1, D2, decision 5). Active when
 * {@code app.providers.sentiment=agent}; {@link StubSentimentEngine} takes the
 * {@code stub} value, and the two are mutually exclusive so exactly one
 * {@link SentimentEngine} bean exists.
 *
 * <p><b>Failure has three shapes and they are handled differently:</b>
 * <ul>
 *   <li><b>Transport failure</b> (network, 5xx, timeout) - throws. {@code @Retry}
 *       retries it, and after retries are exhausted it propagates. The engine
 *       does <i>not</i> swallow this into a NEUTRAL: a story's verdict is written
 *       once and never revisited ({@code sentiment_score.article_id} is unique),
 *       so storing a fabricated NEUTRAL on a transient blip would mislabel the
 *       story permanently. Letting it propagate leaves the story unanalysed for
 *       the next run to retry.</li>
 *   <li><b>A 200 whose text is not the JSON we asked for</b> (the model answered
 *       in prose, or omitted a field) - degrades to NEUTRAL. This is the "engine
 *       cannot decide" case the interface documents, and it stops a model that
 *       keeps returning prose from looping forever.</li>
 *   <li><b>A parseable verdict with bad values</b> (illegal label, score out of
 *       range) - returned as-is for {@link com.fnpis.service.SentimentResultValidator}
 *       to reject. The validator, not this engine, is the gate to the database.</li>
 * </ul>
 *
 * <p><b>The headline is untrusted.</b> It is fenced inside the user message, kept
 * out of the system prompt, and - crucially - the real defence is that nothing
 * reaches the database without passing the validator's schema check. The prompt
 * fence helps; it is not relied on. Neither the raw headline nor the raw model
 * reply is logged in full, since either could carry an injection payload; logs
 * carry lengths, not content.
 */
@Component
@Qualifier("agentSentimentEngine")
@ConditionalOnProperty(name = "app.providers.sentiment", havingValue = "agent")
public class AgentSentimentEngine implements SentimentEngine {

    private static final Logger log = LoggerFactory.getLogger(AgentSentimentEngine.class);

    /** One small JSON object needs very little room; a cap also bounds cost. */
    private static final int MAX_TOKENS = 256;

    /** Reproducible as the model allows (architecture 5.2). */
    private static final int TEMPERATURE = 0;

    /** NEUTRAL fallback claims no certainty - the model gave us nothing to trust. */
    private static final SentimentResult NEUTRAL_FALLBACK = new SentimentResult(
            SentimentLabel.NEUTRAL, BigDecimal.ZERO, BigDecimal.ZERO);

    private final RestClient llm;
    private final ObjectMapper mapper;
    private final String model;
    private final String promptVersion;
    private final String systemPrompt;

    AgentSentimentEngine(
            @Qualifier("llmRestClient") RestClient llm,
            ObjectMapper mapper,
            @Value("${app.sentiment.model}") String model,
            @Value("${app.sentiment.prompt-version}") String promptVersion) {
        this.llm = llm;
        this.mapper = mapper;
        this.model = model;
        this.promptVersion = promptVersion;
        // Load the prompt once at construction. Bundling it in the jar (rather
        // than a config value) keeps it versioned with the code, which is what
        // model_version's prompt half promises to track.
        this.systemPrompt = loadPrompt("prompts/sentiment-" + promptVersion + ".txt");
    }

    @Override
    @RateLimiter(name = "llmSentiment")
    @Retry(name = "externalApi")
    public SentimentResult analyze(String headline) {
        // The engine's contract: headline is never null or blank (the caller
        // filters). A blank one here is an upstream bug, not a headline to score.
        String userMessage = "<headline>\n" + headline + "\n</headline>";

        AnthropicMessageRequest request = new AnthropicMessageRequest(
                model, MAX_TOKENS, TEMPERATURE, systemPrompt,
                List.of(AnthropicMessageRequest.Message.user(userMessage)));

        // A transport failure throws out of here on purpose - @Retry catches it,
        // and an exhausted retry propagates rather than becoming a stored NEUTRAL.
        AnthropicMessageResponse response = llm.post()
                .uri("/v1/messages")
                .body(request)
                .retrieve()
                .body(AnthropicMessageResponse.class);

        return parseVerdict(response);
    }

    @Override
    public String modelVersion() {
        // Model plus prompt version: the only way afterwards to tell which
        // behaviour produced a stored verdict (architecture 4.3).
        return model + "-" + promptVersion;
    }

    /**
     * Turns the model's reply into a verdict, degrading to NEUTRAL when the reply
     * is not the JSON we asked for. A parseable-but-invalid verdict is returned
     * unchanged for the validator to judge.
     */
    private SentimentResult parseVerdict(AnthropicMessageResponse response) {
        String text = response == null ? null : response.firstText();
        if (text == null || text.isBlank()) {
            log.warn("LLM returned no content; treating as NEUTRAL");
            return NEUTRAL_FALLBACK;
        }

        SentimentJson verdict;
        try {
            verdict = mapper.readValue(text.trim(), SentimentJson.class);
        } catch (IOException notOurJson) {
            // The model answered, but not in the schema. Log the length only -
            // the text itself may be an injection payload.
            log.warn("LLM reply was not parseable JSON ({} chars); treating as NEUTRAL",
                    text.length());
            return NEUTRAL_FALLBACK;
        }

        // label maps to null when unknown, so the validator's rule one catches it
        // the same way it catches a missing label. score/confidence pass through
        // untouched for rules two and three.
        return new SentimentResult(
                labelOrNull(verdict.label()), verdict.score(), verdict.confidence());
    }

    /** Known label to its enum, anything else to null for the validator to discard. */
    private SentimentLabel labelOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return SentimentLabel.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    private static String loadPrompt(String path) {
        try {
            return StreamUtils.copyToString(
                    new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException missing) {
            // A missing prompt is a packaging error, not a runtime condition to
            // degrade around: fail construction so the context does not start
            // with an engine that cannot work.
            throw new UncheckedIOException("Sentiment prompt not found on classpath: " + path, missing);
        }
    }
}
