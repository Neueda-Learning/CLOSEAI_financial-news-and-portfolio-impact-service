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
 * <p>Anthropic's Messages API authenticates with {@code x-api-key} and an
 * {@code anthropic-version} header, not a bearer token. The API key is read from
 * {@code llm.api-key} which has no default - a missing key fails startup rather
 * than collecting 401s later (architecture 7.5).
 *
 * <p><b>Multi-provider support:</b> base URL, API key, model, and API version
 * are all injectable via environment variables. No provider is hardcoded.
 * Switch providers by changing {@code LLM_BASE_URL}, {@code LLM_API_KEY},
 * {@code LLM_MODEL}, and optionally {@code LLM_API_VERSION} in {@code .env}.
 */
@Configuration
public class LlmClientConfig {

    /** Default Anthropic Messages API version. Override with LLM_API_VERSION. */
    private static final String DEFAULT_API_VERSION = "2023-06-01";

    @Bean
    public RestClient llmRestClient(
            @Value("${llm.base-url}") String baseUrl,
            @Value("${llm.api-key}") String apiKey,
            @Value("${llm.api-version:2023-06-01}") String apiVersion) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", apiVersion)
                .build();
    }
}
