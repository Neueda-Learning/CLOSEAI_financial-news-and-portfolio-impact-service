package com.fnpis.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Serializes every {@link BigDecimal} as a JSON string.
 *
 * <p>JavaScript numbers are IEEE-754 doubles, so a large monetary value loses
 * precision the moment it crosses the wire as a JSON number. The API contract
 * therefore states money arrives as a string and the frontend only displays
 * it - no arithmetic on that side (architecture 6.2, frontend rule 6.4).
 *
 * <p>Ratios and percentages stay numbers; only BigDecimal-typed fields become
 * strings, which is why money must be BigDecimal end to end.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer bigDecimalAsStringCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
            // ToStringSerializer already emits plain notation, never 1.2E+4
            builder.modulesToInstall(module);
            // Timestamps go out as ISO-8601 UTC, not epoch millis
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }
}
