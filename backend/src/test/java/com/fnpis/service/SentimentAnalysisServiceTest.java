package com.fnpis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fnpis.domain.NewsArticle;
import com.fnpis.domain.SentimentLabel;
import com.fnpis.integration.SentimentEngine;
import com.fnpis.integration.SentimentResult;
import com.fnpis.repository.NewsArticleRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.RestClientException;

/**
 * What one analysis run spends, and what it does when a headline cannot be
 * scored.
 *
 * <p>The failure cases carry the weight here. Every one of them must leave the
 * article with no {@code sentiment_score} row, because that absence is the only
 * thing that makes it eligible for the next run - a row written "just to move
 * on" would be permanent, since the verdict is never revisited.
 */
@ExtendWith(MockitoExtension.class)
class SentimentAnalysisServiceTest {

    private static final int BATCH = 15;
    private static final String MODEL_VERSION = "agent-v1";

    @Mock private NewsArticleRepository articles;
    @Mock private SentimentEngine engine;
    @Mock private SentimentPersistenceService persistence;

    private SentimentAnalysisService service;

    @BeforeEach
    void setUp() {
        // The real validator, not a mock: its four rules are part of the
        // behaviour under test here - a mock would let a bad verdict through and
        // the test would still pass.
        service = new SentimentAnalysisService(
                articles, engine, new SentimentResultValidator(), persistence, BATCH);
    }

    private static NewsArticle article(Long id, String headline) {
        NewsArticle a = new NewsArticle();
        a.setId(id);
        a.setHeadline(headline);
        return a;
    }

    private void backlogOf(NewsArticle... found) {
        when(articles.findUnanalysed(any(Pageable.class))).thenReturn(List.of(found));
    }

    private static SentimentResult verdict(SentimentLabel label, String score) {
        return new SentimentResult(label, new BigDecimal(score), new BigDecimal("0.90"));
    }

    private void storesSuccessfully() {
        when(persistence.persist(anyLong(), any(), anyString())).thenReturn(true);
    }

    @Nested
    @DisplayName("the happy path")
    class HappyPath {

        @Test
        @DisplayName("a scored headline is stored with the engine's model version")
        void storesVerdict() {
            backlogOf(article(1L, "Chipmaker beats guidance"));
            when(engine.analyze("Chipmaker beats guidance"))
                    .thenReturn(verdict(SentimentLabel.POSITIVE, "0.80"));
            when(engine.modelVersion()).thenReturn(MODEL_VERSION);
            storesSuccessfully();

            var result = service.analyseBacklog();

            assertThat(result.stored()).isEqualTo(1);
            assertThat(result.failures()).isZero();
            // The version travels from the engine rather than a constant, so a
            // model swap is visible on the row.
            verify(persistence).persist(eq(1L), any(), eq(MODEL_VERSION));
        }

        @Test
        @DisplayName("an empty backlog costs no LLM calls")
        void emptyBacklogIsNotAnError() {
            when(articles.findUnanalysed(any(Pageable.class))).thenReturn(List.of());

            var result = service.analyseBacklog();

            assertThat(result.triggered()).isTrue();
            assertThat(result.analysed()).isZero();
            verify(engine, never()).analyze(anyString());
        }

        @Test
        @DisplayName("an article a concurrent run already scored counts as neither")
        void duplicateIsNotAFailure() {
            backlogOf(article(1L, "Already scored elsewhere"));
            when(engine.analyze(anyString()))
                    .thenReturn(verdict(SentimentLabel.POSITIVE, "0.50"));
            when(engine.modelVersion()).thenReturn(MODEL_VERSION);
            // Another run inserted the row between our query and our write.
            when(persistence.persist(anyLong(), any(), anyString())).thenReturn(false);

            var result = service.analyseBacklog();

            assertThat(result.analysed()).isEqualTo(1);
            assertThat(result.stored()).isZero();
            // Losing that race is harmless, not a fault to report.
            assertThat(result.failures()).isZero();
            assertThat(result.rejected()).isZero();
        }
    }

    @Nested
    @DisplayName("a run is capped so it cannot drain the quota")
    class BatchCap {

        @Test
        @DisplayName("the configured batch size reaches the query")
        void requestsOnePage() {
            when(articles.findUnanalysed(any(Pageable.class))).thenReturn(List.of());

            service.analyseBacklog();

            var page = org.mockito.ArgumentCaptor.forClass(Pageable.class);
            verify(articles).findUnanalysed(page.capture());
            // An unbounded read would hand the whole backlog to the rate limiter
            // in one pass and spend the day's quota on the first run.
            assertThat(page.getValue().getPageSize()).isEqualTo(BATCH);
            assertThat(page.getValue().getPageNumber()).isZero();
        }
    }

    @Nested
    @DisplayName("failures leave the article queued for the next run")
    class FailuresStayQueued {

        @Test
        @DisplayName("a transport failure stores nothing and does not abort the batch")
        void transportFailureSkipsOnlyThatArticle() {
            backlogOf(article(1L, "Provider is down for this one"),
                    article(2L, "But this one still scores"));
            when(engine.analyze("Provider is down for this one"))
                    .thenThrow(new RestClientException("502 from upstream"));
            when(engine.analyze("But this one still scores"))
                    .thenReturn(verdict(SentimentLabel.NEGATIVE, "-0.40"));
            when(engine.modelVersion()).thenReturn(MODEL_VERSION);
            storesSuccessfully();

            var result = service.analyseBacklog();

            assertThat(result.failures()).isEqualTo(1);
            assertThat(result.stored()).isEqualTo(1);
            // No row for article 1: a fabricated NEUTRAL would be permanent,
            // since a verdict is written once and read forever.
            verify(persistence, never()).persist(eq(1L), any(), anyString());
        }

        @Test
        @DisplayName("a verdict outside the contract is rejected, not stored")
        void invalidVerdictRejected() {
            backlogOf(article(1L, "Score out of range"));
            // 1.5 is off the scale entirely - the validator has nothing to fall
            // back on, so it discards rather than clamping.
            when(engine.analyze(anyString()))
                    .thenReturn(verdict(SentimentLabel.POSITIVE, "1.50"));

            var result = service.analyseBacklog();

            assertThat(result.rejected()).isEqualTo(1);
            assertThat(result.stored()).isZero();
            verify(persistence, never()).persist(anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("a blank headline is skipped without paying for a call")
        void blankHeadlineCostsNothing() {
            backlogOf(article(1L, "   "));

            var result = service.analyseBacklog();

            assertThat(result.rejected()).isEqualTo(1);
            // There is nothing to judge, so no reason to spend the call finding out.
            verify(engine, never()).analyze(anyString());
            verify(persistence, never()).persist(anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("a mislabelled verdict is corrected rather than discarded")
        void signMismatchIsCorrectedNotDropped() {
            backlogOf(article(1L, "Recall widens"));
            // POSITIVE with a negative score: the number is the trustworthy part,
            // so the label is rewritten and the paid-for call is not wasted.
            when(engine.analyze(anyString()))
                    .thenReturn(verdict(SentimentLabel.POSITIVE, "-0.30"));
            when(engine.modelVersion()).thenReturn(MODEL_VERSION);
            storesSuccessfully();

            var result = service.analyseBacklog();

            assertThat(result.stored()).isEqualTo(1);
            var stored = org.mockito.ArgumentCaptor.forClass(SentimentResult.class);
            verify(persistence).persist(anyLong(), stored.capture(), anyString());
            assertThat(stored.getValue().label()).isEqualTo(SentimentLabel.NEGATIVE);
        }
    }

    @Nested
    @DisplayName("the execution lock")
    class Lock {

        @Test
        @DisplayName("a second claim is refused until the first releases")
        void oneRunnerAtATime() {
            assertThat(service.tryAcquire()).isTrue();
            // The manual HTTP trigger and the timer contend for this one lock, so
            // fixedDelay alone is not enough (EC-20, EC-23).
            assertThat(service.tryAcquire()).isFalse();

            service.release();
            assertThat(service.tryAcquire()).isTrue();
        }
    }
}
