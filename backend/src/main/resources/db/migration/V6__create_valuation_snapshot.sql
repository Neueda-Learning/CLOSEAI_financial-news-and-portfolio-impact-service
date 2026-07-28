-- Total value of a portfolio at the close of one session.
--
-- Not in the architecture 4.2 table list, but section 5 has the close snapshot
-- job writing "price_bar plus a portfolio value snapshot", and the value
-- history endpoint behind F5/B5 has no other source. Added here rather than
-- derived on the fly: recomputing history would need every holding as it stood
-- on each past date, which is not recorded anywhere.
--
-- Start writing to this table early. The chart needs several sessions before
-- it shows a line at all, and the fallback path for daily bars has the same
-- lead time (architecture 6.3) - it cannot be caught up the week of the demo.
CREATE TABLE portfolio_valuation_snapshot (
  portfolio_id  BIGINT        NOT NULL,
  snapshot_date DATE          NOT NULL COMMENT 'Session date, no timezone',
  total_value   DECIMAL(18,4) NOT NULL COMMENT 'Sum of holding market values at the close',
  created_at    DATETIME(3)   NOT NULL COMMENT 'UTC',
  -- Composite key: re-running the close job for a session overwrites the row.
  PRIMARY KEY (portfolio_id, snapshot_date),
  CONSTRAINT fk_snapshot_portfolio FOREIGN KEY (portfolio_id)
      REFERENCES portfolio (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
