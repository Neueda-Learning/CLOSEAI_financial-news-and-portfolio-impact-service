package com.fnpis.integration.sentiment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * The body of an Anthropic Messages API call ({@code POST /v1/messages}).
 *
 * <p>An anti-corruption type: it exists so the provider's wire shape stays out
 * of {@link com.fnpis.integration.SentimentEngine} and the service layer. Only
 * {@link AgentSentimentEngine} builds one.
 *
 * <p>The system prompt carries the instructions and the schema; the single user
 * message carries the fenced headline. Splitting them this way keeps the
 * untrusted headline out of the system prompt, where a model weights
 * instructions most heavily.
 *
 * @param model     the model id, from {@code app.sentiment.model}
 * @param maxTokens hard cap on the reply - one small JSON object needs very few
 * @param temperature 0 for the most reproducible answer the model can give
 *                    (architecture 5.2); true reproducibility comes from
 *                    persisting the verdict, not from re-running
 * @param system    the instruction prompt, loaded from {@code prompts/}
 * @param messages  exactly one user turn, the fenced headline
 */
public record AnthropicMessageRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        int temperature,
        String system,
        List<Message> messages) {

    /** One conversational turn. Role is always {@code user} for this engine. */
    public record Message(String role, String content) {

        public static Message user(String content) {
            return new Message("user", content);
        }
    }
}
