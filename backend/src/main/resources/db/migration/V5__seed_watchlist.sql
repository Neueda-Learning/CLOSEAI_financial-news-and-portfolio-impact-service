-- The watchlist (architecture 4.5, checklist item 6). These rows are what the
-- news poll and the quote refresh iterate, and they are also the only symbols
-- a holding may reference - anything else is rejected as unknown (EC-05).
--
-- Fifteen large caps, inside the agreed 10-20 band (AS-03). Chosen for news
-- volume: the demo needs a CONFIRMED assessment to exist on the day (SC-007),
-- and thin coverage is the one way to arrive with nothing to show.
--
-- Company names are seeded rather than fetched so the holdings list renders
-- before module B has run once.
INSERT INTO security (symbol, company_name) VALUES
  ('AAPL',  'Apple Inc.'),
  ('MSFT',  'Microsoft Corporation'),
  ('NVDA',  'NVIDIA Corporation'),
  ('AMZN',  'Amazon.com, Inc.'),
  ('GOOGL', 'Alphabet Inc.'),
  ('META',  'Meta Platforms, Inc.'),
  ('TSLA',  'Tesla, Inc.'),
  ('AMD',   'Advanced Micro Devices, Inc.'),
  ('INTC',  'Intel Corporation'),
  ('JPM',   'JPMorgan Chase & Co.'),
  ('V',     'Visa Inc.'),
  ('WMT',   'Walmart Inc.'),
  ('DIS',   'The Walt Disney Company'),
  ('NFLX',  'Netflix, Inc.'),
  ('BA',    'The Boeing Company');
