package com.fnpis.api.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * One slice of the allocation pie (B3, summary response).
 *
 * @param weight ratio, not percentage: 0.303 means 30.3%. The frontend
 *               multiplies for display (contract 1.2). All slices must sum to
 *               1.0 within 0.0001 (SC-003).
 */
@Schema(description = "A symbol's share of the portfolio")
public record AllocationItem(
        String symbol,
        BigDecimal marketValue,
        Double weight) {
}
