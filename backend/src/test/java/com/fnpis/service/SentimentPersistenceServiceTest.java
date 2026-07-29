package com.fnpis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fnpis.domain.SentimentLabel;
import com.fnpis.domain.SentimentScore;
import com.fnpis.integration.SentimentResult;
import com.fnpis.repository.SentimentScoreRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The write is insert-only, and that is the whole point of this class.
 *
 * <p>Unlike the impact upsert, a verdict is never overwritten: an LLM is not
 * bit-reproducible even at temperature 0, so re-scoring the same headline would
 * change its stored score between reads and destroy the reproducibility that
 * persisting it exists to provide.
 */
@ExtendWith(MockitoExtension.class)
class SentimentPersistenceServiceTest {

    private static final Long ARTICLE_ID = 42L;
    private static final String MODEL_VERSION = "agent-v1";

    @Mock private SentimentScoreRepository scores;

    private SentimentPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new SentimentPersistenceService(scores);
    }

    private static SentimentResult verdict() {
        return new SentimentResult(
                SentimentLabel.NEGATIVE, new BigDecimal("-0.65"), new BigDecimal("0.88"));
    }

    @Test
    @DisplayName("a first verdict is inserted with every field carried across")
    void insertsVerdict() {
        when(scores.findByArticleId(ARTICLE_ID)).thenReturn(Optional.empty());

        assertThat(service.persist(ARTICLE_ID, verdict(), MODEL_VERSION)).isTrue();

        ArgumentCaptor<SentimentScore> saved = ArgumentCaptor.forClass(SentimentScore.class);
        verify(scores).save(saved.capture());
        SentimentScore row = saved.getValue();
        assertThat(row.getArticleId()).isEqualTo(ARTICLE_ID);
        assertThat(row.getLabel()).isEqualTo(SentimentLabel.NEGATIVE);
        assertThat(row.getScore()).isEqualByComparingTo("-0.65");
        assertThat(row.getConfidence()).isEqualByComparingTo("0.88");
        assertThat(row.getModelVersion()).isEqualTo(MODEL_VERSION);
        // analyzed_at is left to @PrePersist rather than set here, so one clock
        // stamps it for every row.
        assertThat(row.getAnalyzedAt()).isNull();
    }

    @Test
    @DisplayName("an article that already has a verdict is left untouched")
    void doesNotOverwrite() {
        SentimentScore existing = new SentimentScore();
        existing.setArticleId(ARTICLE_ID);
        existing.setScore(new BigDecimal("0.20"));
        when(scores.findByArticleId(ARTICLE_ID)).thenReturn(Optional.of(existing));

        assertThat(service.persist(ARTICLE_ID, verdict(), MODEL_VERSION)).isFalse();

        // Re-scoring would move the stored number under a reader who saw the old
        // one, and no re-run should ever pay for the same headline twice.
        verify(scores, never()).save(any());
    }
}
