package com.fnpis.api.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One suggestion for the add-holding symbol field (A4, EC-05).
 *
 * <p>Two fields on purpose. The picker needs the code to submit and the name to
 * recognise it by - nothing else about a security is useful at that moment, and
 * returning the whole entity would tie the widget to the schema.
 *
 * <p>No {@code asOf}/{@code stale} pair here: the watchlist is seeded by
 * {@code V5__seed_watchlist.sql} and never fetched from a provider, so there is
 * no capture time to report and it can never be out of date (contract 1.3,
 * "purely local data").
 */
@Schema(description = "A selectable security")
public record SecurityOption(
        @Schema(description = "股票代码", example = "NVDA") String symbol,
        @Schema(description = "公司名称", example = "NVIDIA Corporation") String companyName) {
}
