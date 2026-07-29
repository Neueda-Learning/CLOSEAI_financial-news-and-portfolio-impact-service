package com.fnpis.integration.mock;

import com.fnpis.integration.DailyBar;
import com.fnpis.integration.PriceProvider;
import com.fnpis.integration.QuoteSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Demo-day safety net: returns fixed quotes without any network calls.
 *
 * <p>Activate by setting {@code PROVIDER_PRICE=mock} in {@code .env}.
 * Full preset data is tracked separately; this skeleton only proves the
 * {@code @ConditionalOnProperty} switch works. Until real presets land,
 * it returns {@code Optional.empty()} for every symbol.
 */
@Component
@Primary
@ConditionalOnProperty(name = "app.providers.price", havingValue = "mock")
public class MockPriceProvider implements PriceProvider {

    @Override
    public Optional<QuoteSnapshot> fetchQuote(String symbol) {
        return Optional.empty(); // TODO: load preset data
    }

    @Override
    public List<DailyBar> fetchDailyBars(String symbol, LocalDate from, LocalDate to) {
        throw new UnsupportedOperationException(
                "Mock daily bars not implemented yet");
    }
}
