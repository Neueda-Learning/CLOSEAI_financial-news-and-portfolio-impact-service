package com.fnpis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A position inside a portfolio (A4).
 *
 * <p><b>Market value, P&amp;L and weight are deliberately absent.</b> All three
 * are derived at read time from quantity, cost basis and the current quote. A
 * stored market value is wrong the moment a price refreshes.
 *
 * <p>One row per (portfolio, symbol), enforced by a unique constraint: a repeat
 * add merges into the existing row and recomputes the weighted average cost
 * (EC-09), so two rows for one position is never a legal state.
 */
@Entity
@Table(name = "holding")
@Getter
@Setter
@NoArgsConstructor
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * Plain FK column rather than a {@code @ManyToOne}.
     *
     * <p>{@code open-in-view} is off, so a lazy association loaded outside a
     * transaction throws. Reads that need the parent join explicitly in their
     * query instead.
     */
    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    /** Whole shares in the MVP (EC-08); the scale leaves room for fractions. */
    @Column(name = "quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    /** Per-share average cost. Zero is legal - gifted stock (EC-07). */
    @Column(name = "cost_basis", nullable = false, precision = 18, scale = 4)
    private BigDecimal costBasis;

    /** UTC. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Holding(Long portfolioId, String symbol, BigDecimal quantity, BigDecimal costBasis) {
        this.portfolioId = portfolioId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.costBasis = costBasis;
    }

    /** Total amount paid for the position. Never a {@code double}. */
    public BigDecimal totalCost() {
        return quantity.multiply(costBasis);
    }

    @PrePersist
    void stampCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
