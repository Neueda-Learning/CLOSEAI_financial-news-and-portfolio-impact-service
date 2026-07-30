package com.fnpis.integration.sentiment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * The reply from the Anthropic Messages API, pared to the one field this engine
 * reads.
 *
 * <p>{@code ignoreUnknown} on purpose: the real response carries usage, stop
 * reason, id and more, none of which the sentiment path uses. Binding only
 * {@code content} means a new field upstream cannot break deserialization.
 *
 * <p>The model's answer is the text of the first content block. The JSON verdict
 * lives inside that text as a string, so {@link AgentSentimentEngine} parses it
 * a second time - the outer envelope is the transport, the inner object is the
 * payload.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnthropicMessageResponse(List<ContentBlock> content) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentBlock(String type, String text) {
    }

    /**
     * The text of the first {@code type=text} block, or null when the reply
     * carried no text content — which {@link AgentSentimentEngine} treats as
     * "cannot tell" and degrades to NEUTRAL rather than throwing.
     *
     * <p>Skips blocks whose {@code text} field is null, which happens when the
     * model provider (DeepSeek) returns a leading {@code thinking} block whose
     * payload is in a {@code thinking} field rather than {@code text}.
     */
    public String firstText() {
        if (content == null || content.isEmpty()) {
            return null;
        }
        for (ContentBlock block : content) {
            if (block.text() != null) {
                return block.text();
            }
        }
        return null;
    }
}
