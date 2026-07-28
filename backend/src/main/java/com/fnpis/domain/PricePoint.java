package com.fnpis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One intraday price observation - a single point on the linked view's curve.
 *
 * <p>Feeds {@code priceSeries.points} on the impact-view response, the most
 * important endpoint in the contract (demo script step 4). {@link PriceQuote}
 * cannot serve it (one row per symbol, overwritten) and neither can
 * {@link PriceBar} (one close per session).
 *
 * <p>Written by the quote refresh job, which must both update the quote and
 * append a point. Rows cannot be back-filled: free providers do not sell
 * historical minute bars, so an unrecorded session is lost.
 */
@Entity
@Table(name = "price_point")
@IdClass(PricePoint.Key.class)
@Getter
@Setter
@NoArgsConstructor
public class PricePoint {

    @Id
    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    /**
     * Provider capture time, not our write time. UTC.
     *
     * <p>Serialised as {@code t} in the contract. {@code newsMarker} is aligned
     * onto this axis, so a value stamped at write time moves the annotation line
     * away from where the price actually moved.
     */
    @Id
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "price", nullable = false, precision = 18, scale = 4)
    private BigDecimal price;

    public PricePoint(String symbol, Instant capturedAt, BigDecimal price) {
        this.symbol = symbol;
        this.capturedAt = capturedAt;
        this.price = price;
    }

    /** Composite primary key. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Key implements Serializable {

        private static final long serialVersionUID = 1L;

        private String symbol;
        private Instant capturedAt;

        public Key(String symbol, Instant capturedAt) {
            this.symbol = symbol;
            this.capturedAt = capturedAt;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return Objects.equals(symbol, key.symbol) && Objects.equals(capturedAt, key.capturedAt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, capturedAt);
        }
    }
}
