package com.fnpis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * The HTTP client for the sentiment LLM (module D), kept apart from module B/C's
 * {@code RestClientConfig} on purpose.
 *
 * <p>The LLM needs its own base URL and its own auth headers - it is a different
 * upstream from Finnhub, on a different key. Folding its bean into the Finnhub
 * config would tie two developers' work to one file and make every merge a
 * conflict. A separate class means module D owns its client end to end (CLAUDE.md
 * "write your own against the shared entities" applied to config).
 *
 * <p>Anthropic's Messages API authenticates with {@code x-api-key} and a pinned
 * {@code anthropic-version}, not a bearer token. Both are set once here so the
 * engine only builds the body. The key is read from {@code llm.api-key}, which
 * has no default - a missing key fails startup rather than collecting 401s later
 * (architecture 7.5).
 */
@Configuration
public class LlmClientConfig {

    /** The Messages API version this code was written against. */
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    @Bean
    public RestClient llmRestClient(
            @Value("${llm.base-url}") String baseUrl,
            @Value("${llm.api-key}") String apiKey) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .build();
    }
}
