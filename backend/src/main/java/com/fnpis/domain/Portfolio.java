package com.fnpis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One investment account (A1).
 *
 * <p>Root of the aggregate: valuation, impact assessment and value snapshots
 * all hang off a portfolio. Deleting one cascades to its holdings and its
 * assessments (A3).
 *
 * <p>No {@code user_id}: the system is single-user (AS-01). The
 * {@code users 1--* portfolios} shape in the repository README is an early
 * draft that was dropped.
 */
@Entity
@Table(name = "portfolio")
@Getter
@Setter
@NoArgsConstructor
public class Portfolio {

    /** USD is the only currency in the MVP (AS-02). */
    public static final String DEFAULT_CURRENCY = "USD";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "base_currency", nullable = false, length = 8)
    private String baseCurrency = DEFAULT_CURRENCY;

    /** UTC. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Portfolio(String name) {
        this.name = name;
    }

    /**
     * Stamps {@code createdAt} when the caller did not.
     *
     * <p>A fallback, not the intended path - the column is NOT NULL, and an
     * insert failing on a timestamp nobody thought about is a poor way to find
     * that out.
     */
    @PrePersist
    void stampCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
