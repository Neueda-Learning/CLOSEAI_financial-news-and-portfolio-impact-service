package com.fnpis.service;

import com.fnpis.api.internal.dto.ImpactRow;
import com.fnpis.domain.ImpactAssessment;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * The one place an {@link ImpactAssessment} row becomes an {@link ImpactRow}.
 *
 * <p>Shared by the list endpoint and the linked view because they publish the
 * same element type (API contract 4.2). Two copies of this mapping is how the
 * wire types drifted from the contract in the first place: a fix applied to one
 * endpoint silently left the other wrong, and the service-layer tests could not
 * see it because they assert on Java fields rather than JSON.
 *
 * <p><b>The type split is the contract (1.2), not a preference.</b> Money stays
 * {@code BigDecimal} so {@code JacksonConfig} emits it as a string and no
 * JavaScript double can round it. Ratios, weights and percentages convert to
 * {@code Double} so they arrive as JSON numbers - the frontend multiplies a
 * weight by 100 for display, which on a quoted string yields concatenation or
 * NaN.
 */
final class ImpactRowMapper {

    /** Percentage scale. Four places keeps 4.1500 rather than rounding to 4.15. */
    private static final int PCT_SCALE = 4;

    /**
     * Weight and contribution scale.
     *
     * <p>Six places, matching the {@code DECIMAL} scale the columns are stored
     * at, so the wire value is the stored value rather than a rounded view of it.
     */
    private static final int RATIO_SCALE = 6;

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private ImpactRowMapper() {
    }

    /**
     * One assessed row, with the company name resolved.
     *
     * <p>A missing name falls back to the symbol rather than null: the row is
     * still a valid assessment, and a blank company column would look like a
     * defect to anyone watching the demo.
     */
    static ImpactRow toRow(ImpactAssessment row, Map<String, String> names) {
        return new ImpactRow(
                row.getSymbol(),
                names.getOrDefault(row.getSymbol(), row.getSymbol()),
                ratio(row.getHoldingWeight()),
                pct(row.getPriceChangeRatio()),
                ratio(row.getExpectedImpact()),
                ratio(row.getObservedContribution()),
                row.getValueImpact(),
                row.getDirection(),
                row.getAlignment());
    }

    /** Ratio to percentage, preserving null - no return means no percentage. */
    static Double pct(BigDecimal value) {
        return value == null
                ? null
                : value.multiply(HUNDRED).setScale(PCT_SCALE, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * A stored ratio as a display number, preserving null.
     *
     * <p>Null survives because it means "not computable" - a missing previous
     * close (EC-18) leaves the contribution unknown, and zero would read as a
     * flat session that was actually never measured.
     */
    static Double ratio(BigDecimal value) {
        return value == null
                ? null
                : value.setScale(RATIO_SCALE, RoundingMode.HALF_UP).doubleValue();
    }
}
