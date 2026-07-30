-- Price data (module B). Reads never call a provider inline; the scheduler
-- lands everything here first and the API serves these rows (decision 2).

-- Latest quote per instrument. One row per symbol, updated in place - history
-- lives in price_bar, so there is nothing to accumulate here.
CREATE TABLE price_quote (
  symbol         VARCHAR(16)   NOT NULL,
  price          DECIMAL(18,4) NOT NULL COMMENT 'Last known price; survives a halt (EC-13)',
  previous_close DECIMAL(18,4)     NULL COMMENT 'Null on a listing under two sessions old (EC-19)',
  -- Provider capture time, not our write time. asOf and stale on every quote
  -- response derive from this, which is the acceptance point for SC-009.
  as_of          DATETIME(3)   NOT NULL COMMENT 'UTC',
  PRIMARY KEY (symbol),
  CONSTRAINT fk_quote_security FOREIGN KEY (symbol)
      REFERENCES security (symbol) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Daily closes, one row per instrument per session. The impact engine reads
-- only this table and never touches an external API: history does not change,
-- so re-fetching it would burn quota for nothing (decision 6).
--
-- If the free tier turns out to expose no daily-candle endpoint, the close
-- snapshot job fills this table from the quote instead (architecture 6.3) -
-- the shape stays the same either way, which is why nothing downstream cares
-- which of the two paths produced a row.
CREATE TABLE price_bar (
  symbol      VARCHAR(16)   NOT NULL,
  trade_date  DATE          NOT NULL COMMENT 'Session date, no timezone (4.4)',
  close_price DECIMAL(18,4) NOT NULL,
  -- Composite key: re-running the close snapshot for a session overwrites the
  -- row rather than appending a second one.
  PRIMARY KEY (symbol, trade_date),
  CONSTRAINT fk_bar_security FOREIGN KEY (symbol)
      REFERENCES security (symbol) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
