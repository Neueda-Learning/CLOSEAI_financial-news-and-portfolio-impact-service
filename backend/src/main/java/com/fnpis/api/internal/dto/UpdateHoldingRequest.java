package com.fnpis.api.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Patch-holding body (A7, P1).
 *
 * <p>Both fields are optional - send the one you want changed. Absent means
 * "leave alone", which is why neither carries {@code @NotNull}. Sending neither
 * is rejected by the service, since a patch that changes nothing is more likely
 * a client bug than an intent.
 *
 * @param quantity  whole shares when present (EC-06, EC-08)
 * @param costBasis per-share cost when present, zero allowed (EC-07)
 */
@Schema(description = "Fields to change on a position; omit what stays as is")
public record UpdateHoldingRequest(

        @Schema(example = "25")
        @Positive(message = "数量必须为正数")
        @Digits(integer = 14, fraction = 0, message = "MVP 只接受整数股")
        BigDecimal quantity,

        @Schema(example = "105.00")
        @PositiveOrZero(message = "成本价不能为负数")
        @Digits(integer = 14, fraction = 4, message = "成本价最多 4 位小数")
        BigDecimal costBasis) {

    /** True when the body carries nothing to change. */
    public boolean isEmpty() {
        return quantity == null && costBasis == null;
    }
}
