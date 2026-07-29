package com.fnpis.api.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Add-holding body (A4).
 *
 * <p>Adding a symbol the portfolio already holds is not an error: the service
 * merges the two into one position and recomputes the weighted average cost
 * (EC-09).
 *
 * @param symbol    must exist in the security table, else 404 (EC-05)
 * @param quantity  whole shares, strictly positive (EC-06, EC-08)
 * @param costBasis per-share cost, zero allowed for gifted stock (EC-07)
 */
@Schema(description = "New position")
public record AddHoldingRequest(

        @Schema(example = "NVDA")
        @NotBlank(message = "股票代码不能为空")
        String symbol,

        // Digits(integer=..., fraction=0) is what rejects 0.5 shares. @Positive
        // alone would accept it, and the column is DECIMAL(18,4) so the database
        // would store it happily.
        @Schema(example = "20")
        @NotNull(message = "数量不能为空")
        @Positive(message = "数量必须为正数")
        @Digits(integer = 14, fraction = 0, message = "MVP 只接受整数股")
        BigDecimal quantity,

        @Schema(example = "100.00")
        @NotNull(message = "成本价不能为空")
        @PositiveOrZero(message = "成本价不能为负数")
        @Digits(integer = 14, fraction = 4, message = "成本价最多 4 位小数")
        BigDecimal costBasis) {

    /** Symbols are stored uppercase (contract 1.2), so normalise at the boundary. */
    public String normalisedSymbol() {
        return symbol == null ? null : symbol.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
