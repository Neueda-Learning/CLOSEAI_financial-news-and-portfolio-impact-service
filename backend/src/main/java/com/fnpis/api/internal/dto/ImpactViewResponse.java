package com.fnpis.api.internal.dto;

import com.fnpis.domain.SentimentLabel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * The linked view: one story, its sentiment, every impacted holding, and one
 * price curve (F4, API contract 4, demo script step 4).
 *
 * <p>Assembled in a single response on purpose - the contract's F4 row says this
 * endpoint alone serves the page, so the frontend never stitches several calls
 * together to draw one chart.
 *
 * <p><b>{@code impacts} is a list; {@code priceSeries} is one curve.</b> The two
 * are bound by {@code selectedSymbol}, and the backend guarantees
 * {@code priceSeries.symbol} equals it. Guessing the curve's owner from
 * {@code impacts[0]} is the documented trap here: the list is ordered by impact
 * magnitude and the caller may have asked for a different symbol entirely.
 *
 * @param article          the story plus its stored verdict
 * @param attributionDate  the session the impacts are charged to
 * @param impactedSymbols  every symbol the story touched, for the switcher
 * @param selectedSymbol   which symbol {@code priceSeries} belongs to
 * @param impacts          one row per impacted holding, biggest mover first
 * @param priceSeries      the intraday curve, null when no points were captured
 * @param asOf             capture time of the oldest input
 * @param stale            true when nothing was computed, or the data is old
 */
@Schema(description = "新闻与价格联动视图，一次取全 (F4)")
public record ImpactViewResponse(
        Article article,
        LocalDate attributionDate,
        List<String> impactedSymbols,
        String selectedSymbol,
        List<ImpactRow> impacts,
        PriceSeries priceSeries,
        Instant asOf,
        boolean stale) {

    /**
     * The story, with the verdict that drove the assessment.
     *
     * @param sentiment null when the analysis job has not reached this headline;
     *                  the view still renders, it just has no direction to show
     */
    @Schema(description = "新闻本身与其情绪判定")
    public record Article(
            Long id,
            String headline,
            String source,
            String url,
            Instant publishedAt,
            Sentiment sentiment) {
    }

    /**
     * The stored verdict, not a fresh one.
     *
     * <p>{@code modelVersion} travels because it is the only way to tell which
     * model and prompt produced this score after either changes.
     */
    @Schema(description = "已落库的情绪判定")
    public record Sentiment(
            SentimentLabel label,
            BigDecimal score,
            BigDecimal confidence,
            String modelVersion) {
    }

    /**
     * One symbol's intraday curve with the news marker already positioned.
     *
     * <p>{@code newsMarker} is the x value for Chart.js's annotation line, aligned
     * to this series' own axis by the backend. The frontend must not re-derive it
     * from {@code publishedAt}: the story's timestamp rarely falls exactly on a
     * captured point, and two independent roundings would put the line and the
     * data on slightly different axes.
     *
     * @param previousClose the prior session's close, for the chart's baseline;
     *                      null when no quote carries one
     * @param newsMarker    null when the story falls outside the captured window
     */
    @Schema(description = "单只股票的日内曲线与新闻标记线")
    public record PriceSeries(
            String symbol,
            BigDecimal previousClose,
            Instant newsMarker,
            List<Point> points) {

        /** {@code t} rather than {@code capturedAt}: the contract's field name. */
        @Schema(description = "一个价格点")
        public record Point(Instant t, BigDecimal price) {
        }
    }
}
