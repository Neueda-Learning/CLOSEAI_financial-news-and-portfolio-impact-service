# Pulsefolio Frontend

Financial news and portfolio impact dashboard for the group project. The current version uses typed mock data, so it can be presented and developed before the backend API contract is ready.

## Run locally

```bash
npm install
npm run dev
```

Production checks:

```bash
npm run lint
npm run build
```

## Current features

- Portfolio overview, market status, and risk summary
- Breaking-news headline paired with the affected asset price movement
- AI sentiment score, confidence, matched company, and impact level
- Interactive holding selector, chart-range tabs, navigation, refresh state, and news search
- Responsive desktop, tablet, and mobile layouts
- Mock data disclaimer for an honest no-backend demo

## Source structure

```text
src/
  App.tsx       # Typed demo data, dashboard components, and interactions
  App.css       # Responsive design system and component styles
  index.css     # Global styles and font setup
  main.tsx      # React entry point
```

## Suggested API contract

Keep API calls in a future `src/services/` directory and map responses into these frontend shapes:

- `GET /api/portfolio` — portfolio totals and holdings
- `GET /api/news?symbols=NVDA,MSFT,AAPL,TSLA` — news with source, timestamp, sentiment, confidence, and matched symbols
- `GET /api/prices/{symbol}?range=1d` — timestamped price series
- `GET /api/impact/events` — correlated news and price-change events

Do not expose a Finnhub key in frontend code. The backend should own external API credentials and return normalized data to this application.
