package com.fnpis.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caffeine caches for the integration layer.
 *
 * <p>Note the scope: this cache sits in front of outbound provider calls, not
 * in front of read endpoints. Read endpoints query the database only - all
 * external data is landed by scheduled jobs first (decision 2). Caching a read
 * endpoint would hide the {@code asOf}/{@code stale} freshness the API
 * contract requires us to report honestly.
 */
@Configuration
@EnableCaching
class CacheConfig {

    /** Latest quote per symbol. Short TTL - this is the hottest path. */
    static final String QUOTES = "quotes";

    /** Daily bars. Historical data never changes, so it can sit for a day. */
    static final String DAILY_BARS = "dailyBars";

    /** Symbol metadata (company name). Effectively static. */
    static final String SECURITIES = "securities";

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(QUOTES, DAILY_BARS, SECURITIES);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(1)));
        manager.registerCustomCache(DAILY_BARS, Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(Duration.ofHours(12))
                .build());
        manager.registerCustomCache(SECURITIES, Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofDays(1))
                .build());
        return manager;
    }
}
