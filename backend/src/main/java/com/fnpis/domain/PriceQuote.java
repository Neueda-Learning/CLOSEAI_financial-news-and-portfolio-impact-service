package com.fnpis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Latest quote for one instrument (B1).
 *
 * <p>One row per symbol, updated in place - history lives in {@link PriceBar},
 * so there is nothing to accumulate here. Reads never call a provider inline;
 * the refresh job lands rows here and the API serves them (decision 2).
 */
@Entity
@Table(name = "price_quote")
@Getter
@Setter
@NoArgsConstructor
public class PriceQuote {

    @Id
    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    /** Last known price. Survives a trading halt (EC-13). */
    @Column(name = "price", nullable = false, precision = 18, scale = 4)
    private BigDecimal price;

    /**
     * Prior session close, or null for a listing under two sessions old
     * (EC-19).
     *
     * <p>Null means the day change cannot be computed. Report it as unavailable
     * rather than substituting zero, which would produce an infinite percentage.
     */
    @Column(name = "previous_close", precision = 18, scale = 4)
    private BigDecimal previousClose;

    /**
     * Provider capture time, not our write time. UTC.
     *
     * <p>Every {@code asOf} and {@code stale} field on a quote response derives
     * from this, so it must carry what the provider reported. Filling it with
     * {@code now()} at write time makes stale data look fresh and breaks the
     * acceptance point for SC-009.
     */
    @Column(name = "as_of", nullable = false)
    private Instant asOf;

    public PriceQuote(String symbol, BigDecimal price, BigDecimal previousClose, Instant asOf) {
        this.symbol = symbol;
        this.price = price;
        this.previousClose = previousClose;
        this.asOf = asOf;
    }

    /** Absolute day change, or null when the prior close is unknown. */
    public BigDecimal change() {
        return previousClose == null ? null : price.subtract(previousClose);
    }
}
