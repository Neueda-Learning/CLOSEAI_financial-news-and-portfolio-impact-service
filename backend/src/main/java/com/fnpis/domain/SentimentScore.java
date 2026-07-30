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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The sentiment verdict on a headline (D1, D2).
 *
 * <p>Written once and read forever. An LLM is not bit-reproducible even at
 * temperature 0, so "reproducible" is delivered by persisting the verdict, not
 * by re-running the model (5.2, decision 5). Every read afterwards uses the
 * stored row.
 *
 * <p>{@link #articleId} is unique, which also stops the analysis job paying for
 * the same headline twice: upsert, and a re-run costs no LLM calls.
 */
@Entity
@Table(name = "sentiment_score")
@Getter
@Setter
@NoArgsConstructor
public class SentimentScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /** Unique in the schema - one verdict per article. */
    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "label", nullable = false, length = 16)
    private SentimentLabel label;

    /** Strength in [-1, 1]. Sign must agree with {@link #label}. */
    @Column(name = "score", nullable = false, precision = 5, scale = 4)
    private BigDecimal score;

    /** Engine confidence in [0, 1]. */
    @Column(name = "confidence", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    /**
     * Model name plus prompt version, e.g. {@code agent-v1}.
     *
     * <p>Multi-engine comparison was dropped, but this stays: after a model or
     * prompt change it is the only way to tell which version produced a verdict
     * (architecture 4.3 point 3).
     */
    @Column(name = "model_version", nullable = false, length = 64)
    private String modelVersion;

    /** UTC. */
    @Column(name = "analyzed_at", nullable = false)
    private Instant analyzedAt;

    @PrePersist
    void stampAnalyzedAt() {
        if (analyzedAt == null) {
            analyzedAt = Instant.now();
        }
    }
}
