package com.fnpis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Total value of a portfolio at the close of one session (B5, F5).
 *
 * <p>Stored rather than derived: recomputing history would need every holding as
 * it stood on each past date, which is recorded nowhere.
 *
 * <p>Start writing rows early. The chart needs several sessions before it shows
 * a line at all, and it cannot be caught up the week of the demo.
 */
@Entity
@Table(name = "portfolio_valuation_snapshot")
@IdClass(PortfolioValuationSnapshot.Key.class)
@Getter
@Setter
@NoArgsConstructor
public class PortfolioValuationSnapshot {

    @Id
    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    /** Session date, no timezone. */
    @Id
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    /** Sum of holding market values at the close. */
    @Column(name = "total_value", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalValue;

    /** UTC. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public PortfolioValuationSnapshot(Long portfolioId, LocalDate snapshotDate, BigDecimal totalValue) {
        this.portfolioId = portfolioId;
        this.snapshotDate = snapshotDate;
        this.totalValue = totalValue;
    }

    @PrePersist
    void stampCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Composite primary key. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Key implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long portfolioId;
        private LocalDate snapshotDate;

        public Key(Long portfolioId, LocalDate snapshotDate) {
            this.portfolioId = portfolioId;
            this.snapshotDate = snapshotDate;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return Objects.equals(portfolioId, key.portfolioId)
                    && Objects.equals(snapshotDate, key.snapshotDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(portfolioId, snapshotDate);
        }
    }
}
