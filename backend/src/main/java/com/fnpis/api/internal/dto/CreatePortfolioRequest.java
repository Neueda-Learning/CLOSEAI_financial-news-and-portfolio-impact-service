package com.fnpis.api.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create-portfolio body (A1).
 *
 * @param name         1-100 characters (EC-10)
 * @param baseCurrency fixed "USD" in the MVP (AS-02); null defaults to USD
 */
@Schema(description = "New portfolio")
public record CreatePortfolioRequest(

        @Schema(example = "美股账户")
        @NotBlank(message = "组合名不能为空")
        @Size(max = 100, message = "组合名不能超过 100 个字符")
        String name,

        // Validated rather than ignored: a client sending "EUR" gets told the MVP
        // does not support it, instead of silently having its value replaced.
        @Schema(example = "USD", defaultValue = "USD")
        @Pattern(regexp = "USD", message = "MVP 只支持 USD")
        String baseCurrency) {

    /** Applies the AS-02 default so the service never sees null. */
    public String baseCurrencyOrDefault() {
        return baseCurrency == null || baseCurrency.isBlank() ? "USD" : baseCurrency;
    }
}
