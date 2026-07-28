-- Core entities: the records the system owns outright, with no external
-- dependency (requirements module A, architecture 4.2).
--
-- Conventions applied across every migration (architecture 4.4):
--   utf8mb4          headlines carry emoji; plain utf8 fails the insert
--   DECIMAL not DOUBLE for money  0.1 + 0.2 must not drift (SC-002)
--   DATETIME(3) in UTC, never TIMESTAMP  TIMESTAMP silently shifts zones

-- A tradable instrument, and at the same time the watchlist: the news poll and
-- the quote refresh iterate exactly the rows present here (architecture 4.5,
-- section 5). Seeded by V5; the MVP does not grow this table at runtime, so an
-- extra in_watchlist flag would carry no information (AS-03, EC-05).
CREATE TABLE security (
  symbol       VARCHAR(16)  NOT NULL COMMENT 'Uppercase ticker, e.g. NVDA',
  company_name VARCHAR(255) NOT NULL,
  PRIMARY KEY (symbol)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- One investment account (A1). Root of the aggregate: valuation, impact
-- assessment and snapshots all hang off a portfolio.
CREATE TABLE portfolio (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  name          VARCHAR(100) NOT NULL COMMENT '1-100 chars, enforced in the API too (EC-10)',
  base_currency VARCHAR(8)   NOT NULL DEFAULT 'USD' COMMENT 'USD only in the MVP (AS-02)',
  created_at    DATETIME(3)  NOT NULL COMMENT 'UTC',
  PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- A position inside a portfolio (A4). Market value, P&L and weight are all
-- derived at read time from quantity, cost_basis and the current quote - none
-- of them is stored, so a price refresh cannot leave stale money behind.
CREATE TABLE holding (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  portfolio_id BIGINT        NOT NULL,
  symbol       VARCHAR(16)   NOT NULL,
  quantity     DECIMAL(18,4) NOT NULL COMMENT 'Whole shares in the MVP (EC-08); scale left for fractions',
  cost_basis   DECIMAL(18,4) NOT NULL COMMENT 'Per-share average cost; 0 is legal for gifted stock (EC-07)',
  created_at   DATETIME(3)   NOT NULL COMMENT 'UTC',
  PRIMARY KEY (id),
  -- EC-09 merges a repeat add into the existing row and recomputes the
  -- weighted average cost. The constraint makes that the only possible
  -- outcome: a service-layer bug surfaces as a failed insert, not as two rows
  -- quietly splitting one position.
  CONSTRAINT uq_holding_portfolio_symbol UNIQUE (portfolio_id, symbol),
  CONSTRAINT fk_holding_portfolio FOREIGN KEY (portfolio_id)
      REFERENCES portfolio (id) ON DELETE CASCADE,
  CONSTRAINT fk_holding_security FOREIGN KEY (symbol)
      REFERENCES security (symbol)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
