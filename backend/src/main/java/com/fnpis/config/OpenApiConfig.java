package com.fnpis.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger metadata. The spec is generated from annotations, never hand-written
 * YAML - once the backend is implemented, Swagger is the single source of
 * truth and the API contract document becomes historical (requirement G3).
 */
@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI fnpisOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Financial News & Portfolio Impact Service")
                .version("v1")
                .description("""
                        Tracks a stock portfolio, pulls company news, scores each \
                        headline for sentiment, and reports whether the price \
                        actually moved the way the news implied.

                        Two response fields appear across most endpoints: `asOf` \
                        is when the underlying data was captured and `stale` \
                        flags that the upstream provider is currently \
                        unavailable or rate limited. Reads are served from the \
                        database, never by calling a provider inline, so the \
                        service stays readable when an upstream is down.

                        Monetary values are serialized as strings to avoid \
                        float precision loss in JavaScript clients.""")
                .license(new License().name("Internal training project")));
    }
}
