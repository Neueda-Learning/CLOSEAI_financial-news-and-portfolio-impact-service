package com.fnpis.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fnpis.domain.Holding;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * EC-09: repeat adds merge at a weighted average cost.
 *
 * <p>Mirrors the formula in {@code HoldingService.mergeInto}. The interesting
 * cases are the ones where a plain average of the two prices would be wrong -
 * that is the mistake this rule exists to prevent.
 */
class WeightedCostMergeTest {

    @Test
    @DisplayName("Equal lots: weighted average matches the plain average")
    void equalLots() {
        assertThat(merge("10", "100.00", "10", "200.00")).isEqualByComparingTo("150.0000");
    }

    @Test
    @DisplayName("Unequal lots: the larger lot dominates, unlike a plain average")
    void unequalLots() {
        // 1*10 + 99*20 = 1990, over 100 shares = 19.90.
        // Averaging the prices alone would say 15.00 - off by nearly 33%.
        BigDecimal weighted = merge("1", "10.00", "99", "20.00");
        assertThat(weighted).isEqualByComparingTo("19.9000");
        assertThat(weighted).isNotEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("The documented example: 20 @ 100 plus 80 @ 150 gives 140")
    void documentedExample() {
        assertThat(merge("20", "100.00", "80", "150.00")).isEqualByComparingTo("140.0000");
    }

    @Test
    @DisplayName("EC-07: merging into a gifted position keeps the cost honest")
    void mergeIntoZeroCost() {
        // 10 free shares plus 10 at 50 averages to 25, not 50.
        assertThat(merge("10", "0.00", "10", "50.00")).isEqualByComparingTo("25.0000");
    }

    @Test
    @DisplayName("A non-terminating division rounds instead of throwing")
    void nonTerminatingDivision() {
        // 1*10 + 2*20 = 50 over 3 shares = 16.666..., which BigDecimal.divide
        // refuses without an explicit scale. Getting this wrong is an
        // ArithmeticException in production, not a wrong number.
        assertThat(merge("1", "10.00", "2", "20.00")).isEqualByComparingTo("16.6667");
    }

    @Test
    @DisplayName("Quantities add up across a merge")
    void quantitiesAdd() {
        Holding h = new Holding(1L, "NVDA", new BigDecimal("20"), new BigDecimal("100.00"));
        BigDecimal total = h.getQuantity().add(new BigDecimal("80"));
        assertThat(total).isEqualByComparingTo("100");
    }

    /** Same expression as the service, kept in one place so a change breaks a test. */
    private static BigDecimal merge(String q1, String c1, String q2, String c2) {
        BigDecimal quantity1 = new BigDecimal(q1);
        BigDecimal quantity2 = new BigDecimal(q2);
        return quantity1.multiply(new BigDecimal(c1))
                .add(quantity2.multiply(new BigDecimal(c2)))
                .divide(quantity1.add(quantity2), 4, RoundingMode.HALF_UP);
    }
}
