package com.fnpis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One instrument's close for one session.
 *
 * <p>The impact engine reads only this table and never an external API: history
 * does not change, so re-fetching it would burn quota for nothing (decision 6).
 * Also the source for the value chart.
 *
 * <p>If the free tier exposes no daily-candle endpoint, the close snapshot job
 * fills this table from the quote instead (architecture 6.3). The shape is the
 * same either way, so nothing downstream cares which path produced a row.
 */
@Entity
@Table(name = "price_bar")
@IdClass(PriceBar.Key.class)
@Getter
@Setter
@NoArgsConstructor
public class PriceBar {

    @Id
    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    /** Session date, no timezone (architecture 4.4). */
    @Id
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "close_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal closePrice;

    public PriceBar(String symbol, LocalDate tradeDate, BigDecimal closePrice) {
        this.symbol = symbol;
        this.tradeDate = tradeDate;
        this.closePrice = closePrice;
    }

    /** Composite primary key. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Key implements Serializable {

        private static final long serialVersionUID = 1L;

        private String symbol;
        private LocalDate tradeDate;

        public Key(String symbol, LocalDate tradeDate) {
            this.symbol = symbol;
            this.tradeDate = tradeDate;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return Objects.equals(symbol, key.symbol) && Objects.equals(tradeDate, key.tradeDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, tradeDate);
        }
    }
}
