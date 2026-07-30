package com.fnpis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What one news story did to one holding on one session (E1-E4).
 *
 * <p>The project's only original logic, and the reason the linked view has
 * anything to show. Computed by the scheduler and stored, never derived inside a
 * read request: the numbers depend on prices at a point in time, so recomputing
 * on read would answer differently every call and nothing would be reproducible
 * on demo day (decision 3).
 *
 * <p><b>Ratios are stored as ratios, not percentages.</b>
 * {@code priceChangeRatio} 0.0415 is the +4.15% the API renders - the x100
 * happens once, on the way out. Multiplying here as well yields 415%.
 *
 * <p>Deliberately no association to {@link Holding}: a row records what a story
 * did to a symbol in a portfolio, not to one holding row. Deleting a position
 * (A6) must not erase history already assessed against it.
 */
@Entity
@Table(name = "impact_assessment")
@Getter
@Setter
@NoArgsConstructor
public class ImpactAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    /** Value impact depends on this portfolio's weights, so it is part of the key. */
    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    /** The session the story is charged to (requirements 5.3 step 1). */
    @Column(name = "attribution_date", nullable = false)
    private LocalDate attributionDate;

    /** w: this holding's value over the portfolio's total. */
    @Column(name = "holding_weight", nullable = false, precision = 10, scale = 6)
    private BigDecimal holdingWeight;

    /**
     * r: the session's price move as a ratio. Null when the prior close is
     * missing (EC-18), which forces {@link Alignment#INCONCLUSIVE}.
     */
    @Column(name = "price_change_ratio", precision = 10, scale = 6)
    private BigDecimal priceChangeRatio;

    /** s * c * w, signed. What the sentiment predicted. */
    @Column(name = "expected_impact", nullable = false, precision = 10, scale = 6)
    private BigDecimal expectedImpact;

    /** w * r. What the market actually did. Null when r is null. */
    @Column(name = "observed_contribution", precision = 10, scale = 6)
    private BigDecimal observedContribution;

    /** Holding value * r - the money figure shown on screen. Null when r is null. */
    @Column(name = "value_impact", precision = 18, scale = 4)
    private BigDecimal valueImpact;

    /** Direction the sentiment implied (E2). */
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 24)
    private Direction direction;

    /** Whether the price agreed with that direction (E4). */
    @Enumerated(EnumType.STRING)
    @Column(name = "alignment", nullable = false, length = 24)
    private Alignment alignment;

    /** UTC. Answers "when was this concluded". */
    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @PrePersist
    void stampComputedAt() {
        if (computedAt == null) {
            computedAt = Instant.now();
        }
    }
}
