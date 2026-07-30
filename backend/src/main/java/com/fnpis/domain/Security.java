package com.fnpis.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A tradable instrument, and at the same time the watchlist.
 *
 * <p>The news poll and the quote refresh iterate exactly the rows in this
 * table, and a holding may only reference a symbol present here - anything else
 * is rejected as unknown (EC-05). Seeded by {@code V5__seed_watchlist.sql}; the
 * MVP does not grow it at runtime.
 *
 * <p>The symbol is the primary key, not a surrogate id: tickers are already
 * unique and every other table joins on them.
 */
@Entity
@Table(name = "security")
@Getter
@Setter
@NoArgsConstructor
public class Security {

    @Id
    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    public Security(String symbol, String companyName) {
        this.symbol = symbol;
        this.companyName = companyName;
    }
}
