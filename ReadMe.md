# Financial News & Portfolio Impact Service (FNPIS)

> **Project #15 · Group 7** — Aggregates financial news for companies in a portfolio and estimates which holdings are affected by integrating with both a financial news API and a stock price API. Uses NLP sentiment analysis to correlate news with intraday price movements (5-min polling).

[中文版 (Chinese)](./ReadMe.zh-CN.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Team](#team)
3. [Tech Stack](#tech-stack)
4. [Project Structure](#project-structure)
5. [Getting Started](#getting-started)
6. [REST API Documentation](#rest-api-documentation)
7. [Database Schema](#database-schema)
8. [External API Integration](#external-api-integration)
9. [NLP Sentiment Analysis](#nlp-sentiment-analysis)
10. [Frontend Pages](#frontend-pages)
11. [Scheduled Jobs](#scheduled-jobs)
12. [Testing](#testing)
13. [CI/CD & Docker](#cicd--docker)
14. [Git Workflow](#git-workflow)
15. [Project Management](#project-management)
16. [Presentation Plan](#presentation-plan)

---

## Overview

### The Problem

Investors hold portfolios of stocks but struggle to connect the dots between breaking financial news and its actual impact on their holdings. A headline about Apple cutting production forecasts should immediately signal a potential price drop — but most investors discover the connection hours too late.

### Our Solution

FNPIS is a full-stack web application that:

1. **Aggregates** financial news for every company in a user's portfolio via the Finnhub API
2. **Analyzes** each news article's sentiment (Positive / Negative / Neutral) using NLP
3. **Correlates** news sentiment with intraday stock price movements (5-min polling)
4. **Visualizes** the impact side-by-side — news on the left, price chart on the right

### Core Features (Priority Order)

| Priority | Feature | Description | Acceptance Criteria |
|----------|---------|-------------|---------------------|
| P0 | Browse Records | View portfolio holdings, news feed, and impact events | User sees a list of all holdings with ticker, shares, and current price; news feed loads ≤3 seconds |
| P1 | View Metrics | Graphical dashboard: sentiment trends, price-impact correlations, allocation charts | Dashboard renders ≥2 chart types with data from the last 30 days |
| P2 | Add Items | Add holdings to portfolio, trigger news fetch | User enters ticker + shares → holding appears in portfolio → news fetched within 15 minutes |
| P3 | Remove Items | Remove holdings, dismiss impact events, clear historical data | Removed holding disappears from dashboard; associated impact events are soft-deleted |

### Integration Requirement

FNPIS integrates with **two** external APIs (exceeding the minimum of one):

| API | Purpose | Auth |
|-----|---------|------|
| [Finnhub](https://finnhub.io/) | Financial news (1 year history) + Stock prices (intraday & historical) | Free API key |
| [Alpha Vantage](https://www.alphavantage.co/) | Supplementary stock prices (fallback) | Free API key |

---

## Team

**Team Name:** CLOSEAI

| Name | Role | Responsibilities |
|------|------|-----------------|
| Evan Li | TBD | TBD |
| David Hu | TBD | TBD |
| Venessa Feng | TBD | TBD |
| Ethan SUN | TBD | TBD |
| Timothy Xue | TBD | TBD |

> Role assignments (Backend Lead / NLP Lead / Frontend Lead / additional roles) to be decided by the team in Week 1.

**Instructors (GitHub Viewers):** `helppo2`, `tuistmessiah`

### Team Working Approach

- **Self-organising** — the team decides how to divide work (e.g. 2 backend + 1 frontend, or everyone on backend until MVP works, or pair programming as suggested in the project guidelines)
- **Regular check-ins** — schedule daily syncs to keep good energy and unblock each other
- **One repository** — all work lives in a single GitHub repo (public or shared with instructors and all team members)
- **Raise blockers early** — if stuck, bring issues to instructors before they become delays

---

## Tech Stack

| Layer | Technology | Rationale |
|-------|-----------|-----------|
| **Backend** | Java 17 + Spring Boot 3 | Training stack |
| **Frontend** | React + Chart.js / D3.js | SPA with rich interactive charts |
| **Database** | MySQL 8 | Persistent storage for holdings, news, prices, impact events |
| **NLP** | finBERT (ProsusAI) via Python microservice, or LLM API | Financial-domain sentiment analysis; local-first for reliability |
| **External APIs** | Finnhub (primary), Alpha Vantage (fallback) | News + stock prices |
| **Scheduling** | Spring `@Scheduled` | Periodic news fetch, price polling, impact correlation |
| **API Docs** | Swagger / OpenAPI 3.0 | Auto-generated from annotations |
| **CI/CD** | GitHub Actions | Build → Test → Lint on every PR |
| **Container** | Docker + Docker Compose | One-command local setup; portable deployment |
| **Version Control** | Git + GitHub | Feature branches, PR reviews |
| **Project Mgmt** | [Jira](https://therain2026.atlassian.net/jira/software/projects/FNPIS/boards/3) | Task tracking, Kanban board |

---

## Project Structure

```
FNPIS/
├── backend/
│   ├── src/
│   │   ├── main/java/com/fnpis/
│   │   │   ├── controller/          # REST API endpoints
│   │   │   │   ├── PortfolioController.java
│   │   │   │   ├── HoldingController.java
│   │   │   │   ├── NewsController.java
│   │   │   │   ├── ImpactController.java
│   │   │   │   └── DashboardController.java
│   │   │   ├── service/             # Business logic
│   │   │   │   ├── PortfolioService.java
│   │   │   │   ├── NewsFetchService.java
│   │   │   │   ├── SentimentService.java
│   │   │   │   ├── PriceService.java
│   │   │   │   └── ImpactCorrelatorService.java
│   │   │   ├── model/               # JPA entities / ORM models
│   │   │   │   ├── User.java
│   │   │   │   ├── Portfolio.java
│   │   │   │   ├── Holding.java
│   │   │   │   ├── NewsArticle.java
│   │   │   │   ├── SentimentScore.java
│   │   │   │   ├── StockPrice.java
│   │   │   │   └── ImpactEvent.java
│   │   │   ├── repository/          # Data access layer
│   │   │   ├── config/              # App config, API keys, Swagger
│   │   │   └── scheduler/           # Cron job definitions
│   │   └── resources/
│   │       └── application.yml      # DB config, API keys (env vars)
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── PortfolioDashboard/
│   │   │   ├── NewsFeed/
│   │   │   ├── ImpactViewer/        # Side-by-side news + price chart
│   │   │   ├── HoldingManager/
│   │   │   └── common/              # Charts, cards, nav
│   │   ├── pages/
│   │   │   ├── DashboardPage.jsx
│   │   │   ├── NewsImpactPage.jsx
│   │   │   └── PortfolioPage.jsx
│   │   ├── services/                # API client calls
│   │   └── App.jsx
│   ├── Dockerfile
│   └── package.json
├── nlp-service/                     # Python microservice (if using finBERT)
│   ├── app.py                       # Flask/FastAPI sentiment endpoint
│   ├── model.py                     # finBERT model loader + inference
│   ├── requirements.txt
│   └── Dockerfile
├── docker-compose.yml               # Orchestrates backend + frontend + DB + nlp
├── .github/
│   └── workflows/
│       └── ci.yml                   # Build, lint, test pipeline
└── ReadMe.md
```

---

## Getting Started

### Prerequisites

- **Java 17+** + Maven
- **Python 3.10+** (for NLP microservice)
- **Docker & Docker Compose**
- **MySQL 8+** (or use Dockerized DB)
- **Finnhub API Key** — [Get free key](https://finnhub.io/register)

### Environment Variables

Create a `.env` file in the project root (never commit this file):

```env
# Database
DB_HOST=localhost
DB_PORT=3306
MYSQL_DATABASE=fnpis
MYSQL_USER=fnpis_user
MYSQL_PASSWORD=your_db_password
MYSQL_ROOT_PASSWORD=your_root_password

# External APIs
FINNHUB_API_KEY=your_finnhub_key
ALPHA_VANTAGE_API_KEY=your_alphavantage_key

# NLP Service
NLP_SERVICE_URL=http://localhost:5001

# App
SERVER_PORT=8080
```

### Quick Start (Docker)

```bash
# 1. Clone the repo
git clone https://github.com/Neueda-Learning/CLOSEAI_financial-news-and-portfolio-impact-service.git
cd FNPIS

# 2. Set up environment
cp .env.example .env
# Edit .env with your API keys

# 3. Start everything
docker-compose up -d

# 4. Verify
# Backend API docs:  http://localhost:8080/swagger-ui.html
# Frontend:         http://localhost:3000
# NLP Service:      http://localhost:5001/health
```

### Manual Start (Development)

```bash
# Terminal 1 — Database
docker-compose up -d db

# Terminal 2 — NLP Service
cd nlp-service
pip install -r requirements.txt
python app.py                        # Runs on :5001

# Terminal 3 — Backend
cd backend
./mvnw spring-boot:run               # Runs on :8080

# Terminal 4 — Frontend
cd frontend
npm install
npm run dev                          # Runs on :3000
```

---

## REST API Documentation

Full interactive docs available at `http://localhost:8080/swagger-ui.html` after startup.

> **Bonus Objective:** Expose the API so instructors can query system information during the final presentation (e.g. `GET /api/portfolios` to verify holdings, `GET /api/impact?portfolioId=1` to inspect impact events). Swagger UI serves as both documentation and a live inspection tool for this purpose.

### Base URL

```
http://localhost:8080/api
```

### Endpoints

#### Portfolio

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/portfolios` | List user's portfolios |
| `POST` | `/portfolios` | Create a portfolio |
| `GET` | `/portfolios/{id}` | Get portfolio with holdings, total value |
| `PUT` | `/portfolios/{id}` | Update portfolio name |
| `DELETE` | `/portfolios/{id}` | Delete a portfolio |

#### Holdings

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/portfolios/{id}/holdings` | List holdings in a portfolio |
| `POST` | `/portfolios/{id}/holdings` | Add a holding `{ticker, shares, avgCost}` |
| `PUT` | `/holdings/{id}` | Update holding (shares/cost) |
| `DELETE` | `/holdings/{id}` | Remove a holding |

#### News

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/news?ticker=AAPL&days=7` | News for a ticker (date range) |
| `GET` | `/news/portfolio/{portfolioId}` | All news for a portfolio's holdings |
| `GET` | `/news/{id}/sentiment` | Sentiment analysis result for one article |

#### Stock Prices

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/prices?tickers=AAPL,TSLA` | Current prices (batch) |
| `GET` | `/prices/{ticker}/history?range=1m` | Historical prices for charts |

#### Impact Analysis (Core)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/impact?portfolioId=1&days=30` | All impact events for a portfolio |
| `GET` | `/impact/{id}` | Single impact event detail (news + chart data) |
| `GET` | `/impact/{id}/comparison` | Side-by-side: news text vs price chart data |
| `POST` | `/impact/calculate?portfolioId=1` | Manually trigger impact correlation |

#### Dashboard

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/dashboard/summary?portfolioId=1` | Portfolio summary + today's sentiment |
| `GET` | `/dashboard/sentiment-trend?portfolioId=1&days=30` | 30-day sentiment trend data |
| `GET` | `/dashboard/top-impact?portfolioId=1&limit=5` | Top 5 news-impact events |

### Example Response: Impact Event

```json
{
  "id": 142,
  "newsArticle": {
    "id": 891,
    "headline": "Apple cuts iPhone production forecast by 10M units",
    "summary": "Citing supply chain constraints, Apple Inc. has reduced...",
    "source": "Reuters",
    "publishedAt": "2026-07-27T09:30:00Z",
    "url": "https://www.reuters.com/..."
  },
  "sentiment": {
    "label": "NEGATIVE",
    "confidence": 0.97,
    "score": -2
  },
  "holding": {
    "ticker": "AAPL",
    "shares": 200,
    "avgCost": 175.50
  },
  "priceImpact": {
    "priceBeforeNews": 195.30,
    "priceAfterNews": 188.70,
    "priceChangePct": -3.38,
    "timeWindow": "T -> T+2h"
  },
  "correlation": {
    "strength": "STRONG",
    "direction": "ALIGNED",
    "summary": "Strong negative correlation: Negative news aligned with -3.38% price drop"
  }
}
```

---

## Database Schema

```sql
-- Users (simplified — single user initially per PDF note)
CREATE TABLE users (
    id          SERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Portfolios
CREATE TABLE portfolios (
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Holdings (positions in a portfolio)
CREATE TABLE holdings (
    id              SERIAL PRIMARY KEY,
    portfolio_id    INTEGER NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    ticker          VARCHAR(10)   NOT NULL,
    company_name    VARCHAR(200),
    shares          DECIMAL(15,6) NOT NULL CHECK (shares > 0),
    avg_cost        DECIMAL(15,4) NOT NULL CHECK (avg_cost > 0),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (portfolio_id, ticker)
);

-- News articles fetched from Finnhub
CREATE TABLE news_articles (
    id            SERIAL PRIMARY KEY,
    ticker        VARCHAR(10)  NOT NULL,
    headline      TEXT         NOT NULL,
    summary       TEXT,
    source        VARCHAR(100),
    url           TEXT,
    published_at  TIMESTAMP    NOT NULL,
    fetched_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_news_ticker ON news_articles(ticker);
CREATE INDEX idx_news_published ON news_articles(published_at);

-- Sentiment analysis results (one per article)
CREATE TABLE sentiment_scores (
    id            SERIAL PRIMARY KEY,
    news_id       INTEGER NOT NULL UNIQUE REFERENCES news_articles(id) ON DELETE CASCADE,
    sentiment     VARCHAR(10)  NOT NULL CHECK (sentiment IN ('POSITIVE', 'NEGATIVE', 'NEUTRAL')),
    confidence    DECIMAL(5,4) NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
    score         INTEGER NOT NULL CHECK (score BETWEEN -2 AND 2),
    analyzed_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sentiment_label ON sentiment_scores(sentiment);

-- Stock prices (cached from external API, prevents rate-limiting)
CREATE TABLE stock_prices (
    id        SERIAL PRIMARY KEY,
    ticker    VARCHAR(10)    NOT NULL,
    open      DECIMAL(15,4),
    high      DECIMAL(15,4),
    low       DECIMAL(15,4),
    close     DECIMAL(15,4)  NOT NULL,
    volume    BIGINT,
    timestamp TIMESTAMP      NOT NULL,
    UNIQUE (ticker, timestamp)
);

CREATE INDEX idx_price_ticker_time ON stock_prices(ticker, timestamp);

-- Impact events (core: links news sentiment to price movement)
CREATE TABLE impact_events (
    id                  SERIAL PRIMARY KEY,
    news_id             INTEGER NOT NULL REFERENCES news_articles(id) ON DELETE CASCADE,
    holding_id          INTEGER NOT NULL REFERENCES holdings(id) ON DELETE CASCADE,
    ticker              VARCHAR(10)  NOT NULL,
    sentiment           VARCHAR(10)  NOT NULL,
    price_before_news   DECIMAL(15,4),
    price_after_news    DECIMAL(15,4),
    price_change_pct    DECIMAL(8,4),
    time_window         VARCHAR(20) DEFAULT 'T+2h',
    correlation_strength VARCHAR(10) CHECK (correlation_strength IN ('STRONG', 'MEDIUM', 'WEAK', 'NONE')),
    correlation_direction VARCHAR(10) CHECK (correlation_direction IN ('ALIGNED', 'DIVERGENT', 'NEUTRAL')),
    summary             TEXT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_impact_ticker ON impact_events(ticker);
CREATE INDEX idx_impact_created ON impact_events(created_at);
```

### Entity Relationship Diagram

```
users 1--* portfolios 1--* holdings
                                  |
news_articles 1--1 sentiment_scores
       |                        |
       +-------- impact_events -+
                      |
              stock_prices (referenced by ticker + timestamp)
```

---

## External API Integration

### Finnhub (Primary)

| Endpoint | Usage | Rate Limit |
|----------|-------|------------|
| `/api/v1/news?category=general` | General market news | 60 req/min (free) |
| `/api/v1/company-news?symbol=AAPL&from=2026-01-01&to=2026-07-27` | Company-specific news (1 year history) | 60 req/min |
| `/api/v1/quote?symbol=AAPL` | Real-time quote (c, h, l, o, pc) | 60 req/min |
| `/api/v1/stock/candle?symbol=AAPL&resolution=D&from=...&to=...` | Historical candles | 60 req/min |

**Rate Limit Strategy:** Cache all responses in the `stock_prices` and `news_articles` tables. Never call the API directly from the frontend. Scheduled jobs fetch data and populate the local cache.

**Fallback Strategy:** If Finnhub returns 429 (rate limited) or 5xx, the system reads from the local cache. If the cache is stale (> 15 minutes for prices, > 1 hour for news), the UI displays a "Data may be delayed" banner.

### Alpha Vantage (Fallback)

| Endpoint | Usage |
|----------|-------|
| `GLOBAL_QUOTE&symbol=AAPL` | Real-time quote fallback |
| `TIME_SERIES_DAILY&symbol=AAPL` | Historical data fallback |

---

## NLP Sentiment Analysis

### Approach: finBERT (local, open-source)

[finBERT](https://github.com/ProsusAI/finBERT) is a BERT model fine-tuned on financial text (SEC filings, earnings reports, analyst notes). It outperforms general sentiment models on financial-domain text.

**Why local instead of LLM API:**

1. No API cost at any scale
2. No rate limiting — process hundreds of articles instantly
3. Works offline — demo won't fail if internet drops
4. Financial-domain accuracy (finBERT understands "beat estimates" vs "missed earnings")

### Sentiment Service (Python + Flask)

```python
# nlp-service/app.py
from flask import Flask, request, jsonify
from transformers import pipeline

app = Flask(__name__)

# Load once at startup
classifier = pipeline(
    "sentiment-analysis",
    model="ProsusAI/finbert"
)

def map_to_score(result):
    """Map finBERT output to -2..+2 scale."""
    label = result["label"].lower()
    score = result["score"]
    if label == "positive":
        return {"sentiment": "POSITIVE", "confidence": score, "score": 2 if score > 0.8 else 1}
    elif label == "negative":
        return {"sentiment": "NEGATIVE", "confidence": score, "score": -2 if score > 0.8 else -1}
    else:
        return {"sentiment": "NEUTRAL", "confidence": score, "score": 0}

@app.route("/analyze", methods=["POST"])
def analyze():
    data = request.get_json()
    text = f"{data.get('headline', '')}. {data.get('summary', '')}"
    results = classifier(text[:512])  # finBERT max token limit
    return jsonify(map_to_score(results[0]))

@app.route("/analyze/batch", methods=["POST"])
def analyze_batch():
    """Batch mode: process multiple articles at once."""
    articles = request.get_json().get("articles", [])
    results = []
    for article in articles:
        text = f"{article.get('headline', '')}. {article.get('summary', '')}"
        r = classifier(text[:512])[0]
        results.append(map_to_score(r))
    return jsonify(results)

@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "model": "ProsusAI/finbert"})

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5001)
```

### Fallback: LLM API (if finBERT is unavailable)

```java
// Backend fallback — call LLM API for sentiment
// Only used if the NLP microservice is unreachable
public SentimentResult analyzeViaLLM(String headline, String summary) {
    String prompt = String.format("""
        Classify this financial news headline.
        Reply with EXACTLY one word: POSITIVE, NEGATIVE, or NEUTRAL.

        Headline: %s
        Summary: %s
        """, headline, summary != null ? summary : "");

    // Call DeepSeek / OpenAI / Claude API
    String response = llmClient.complete(prompt);
    return parseSentiment(response);
}
```

---

## Frontend Pages

### Page 1: Portfolio Dashboard

```
+----------------------------------------------------------+
|  FNPIS — Financial News & Portfolio Impact               |
|                                                          |
|  Portfolio: My Tech Portfolio    Total: $127,350          |
|                                                          |
|  +-- Holdings -----------------------------------------+ |
|  | Ticker  Shares   Price    Value    Change            | |
|  | AAPL    200      $195.30  $39,060  +1.2%            | |
|  | TSLA    80       $245.10  $19,608  -2.1%            | |
|  | MSFT    150      $458.20  $68,730  +0.5%            | |
|  +-----------------------------------------------------+ |
|                                                          |
|  +-- Today's Sentiment --------------------------------+ |
|  | GREEN  POSITIVE  12 articles                         | |
|  | RED    NEGATIVE   5 articles                         | |
|  | GRAY   NEUTRAL     3 articles                        | |
|  +-----------------------------------------------------+ |
|                                                          |
|  [+ Add Holding]  [View Impact Feed ->]                   |
+----------------------------------------------------------+
```

### Page 2: News & Impact Feed (Core Demo Page)

```
+--------------------------------------------------------------+
|  News & Impact Feed                              Last 7 days  |
|                                                              |
|  +-- Top Impact Events ------------------------------------+ |
|  |                                                        | |
|  |  RED  AAPL  -3.38%   STRONG correlation                 | |
|  |  +----------------------------------------------------+ | |
|  |  | "Apple cuts iPhone production forecast             | | |
|  |  |  by 10M units" — Reuters                           | | |
|  |  |  Sentiment: NEGATIVE (97%)                         | | |
|  |  |  $195.30 -> $188.70  |  -$6.60 per share           | | |
|  |  +----------------------------------------------------+ | |
|  |                                                        | |
|  |  GREEN  MSFT  +1.82%   MEDIUM correlation               | |
|  |  +----------------------------------------------------+ | |
|  |  | "Microsoft beats Q4 earnings estimates"            | | |
|  |  | Sentiment: POSITIVE (89%)                          | | |
|  |  | $452.10 -> $460.30  |  +$8.20 per share            | | |
|  |  +----------------------------------------------------+ | |
|  +--------------------------------------------------------+ |
|                                                              |
|  Sentiment Trend (30d)                                       |
|  +--------------------------------------------------------+  |
|  |  jul 1                                      jul 27    |  |
|  +--------------------------------------------------------+  |
+--------------------------------------------------------------+
```

### Page 3: Side-by-Side Comparison (Presentation Hook)

```
+--------------------------------+--------------------------------+
|  News Analysis                 |  Price Chart (AAPL)            |
|                                |                                |
|  RED  NEGATIVE (97%)           |  $196 -|                       |
|                                |  $194 -|    \                  |
|  "Apple cuts iPhone            |  $192 -|     \                 |
|   production forecast          |  $190 -|      \_____           |
|   by 10M units"                |  $188 -|            \___       |
|                                |  $186 -|                \___   |
|  Source: Reuters               |        |----|----|----|----|   |
|  Published: 2026-07-27         |      9:00 10:00 11:00 12:00  |
|  09:30 AM ET                   |           |                    |
|                                |      News published            |
|  Impact: -3.38%                |                                |
|  Correlation: STRONG (check)   |  Price before: $195.30        |
|  Direction: ALIGNED            |  Price after:  $188.70        |
|                                |  Drop:  -$6.60 (-3.38%)       |
+--------------------------------+--------------------------------+
```

---

## Scheduled Jobs

| Job | Frequency | Description |
|-----|-----------|-------------|
| `NewsFetcher` | Every 15 min | Fetch latest news from Finnhub for all tickers in all portfolios |
| `PricePoller` | Every 5 min | Fetch real-time quotes for all tracked tickers, cache to `stock_prices` |
| `SentimentAnalyzer` | After NewsFetcher | Run NLP sentiment analysis on any unanalyzed articles |
| `ImpactCorrelator` | Every hour | For new articles: find nearest price before/after, calculate impact, write `impact_events` |
| `CacheCleaner` | Daily at 03:00 | Purge price data older than 90 days, news older than 1 year |

---

## Testing

### Unit Tests

```
Backend (JUnit 5 + Mockito):
  - Service layer: PortfolioService, SentimentService, ImpactCorrelatorService
  - Impact correlation algorithm: edge cases (no price data, single-sided news)
  - Data validation: ticker format, share amounts, percentage ranges

NLP Service (pytest):
  - finBERT inference: known positive/negative/neutral headlines
  - Score mapping: confidence thresholds -> score ranges
  - Batch processing: correct N results for N inputs

Frontend (Jest + React Testing Library):
  - Component rendering: dashboard cards, impact event rows
  - API mock responses
```

### End-to-End Tests (Cypress / Playwright)

```
1. Add a holding -> verify it appears in portfolio
2. Trigger news fetch -> verify news feed populates
3. Verify sentiment labels appear on news articles
4. Trigger impact calculation -> verify impact events generated
5. Open side-by-side view -> verify chart renders with news annotation
6. Remove a holding -> verify cleanup
```

---

## CI/CD & Docker

### CI Pipeline (`.github/workflows/ci.yml`)

| Job | What It Runs | Triggers |
|-----|-------------|----------|
| `lint` | ESLint (backend + frontend), Checkstyle (Java) or Prettier | PR to `dev` / `master`; push to `dev` / `master` |
| `type-check` | `tsc --noEmit` (TypeScript) or `javac` (Java) | PR to `dev` / `master` |
| `unit-test` | Backend: JUnit / Jest; Frontend: Jest + React Testing Library; NLP: pytest | PR to `dev` / `master` |
| `build` | Verify the project compiles / bundles without errors | PR to `dev` / `master` |
| `commitlint` | Reject commits that don't follow Conventional Commits | PR to `dev` / `master` |

> **Critical rule:** CI must trigger on **both** PR and push to `dev`/`master`. The push trigger catches post-merge failures when two PRs pass individually but break `dev` when combined (see [Dev CI Failure](#dev-ci-failure)).

### Docker Compose (`docker-compose.yml`)

```yaml
version: "3.8"
services:
  db:
    image: mysql:8
    environment:
      MYSQL_DATABASE: fnpis
      MYSQL_USER: fnpis_user
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    volumes:
      - mysqldata:/var/lib/mysql
    ports:
      - "3306:3306"

  nlp-service:
    build: ./nlp-service
    ports:
      - "5001:5001"
    restart: unless-stopped

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - DB_HOST=db
      - DB_PORT=3306
      - NLP_SERVICE_URL=http://nlp-service:5001
      - FINNHUB_API_KEY=${FINNHUB_API_KEY}
    depends_on:
      - db
      - nlp-service
    restart: unless-stopped

  frontend:
    build: ./frontend
    ports:
      - "3000:3000"
    depends_on:
      - backend
    restart: unless-stopped

volumes:
  mysqldata:
```

### GitHub Actions CI (`ci.yml`)

```yaml
name: CI Pipeline
on: [push, pull_request]
jobs:
  backend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run backend tests
        run: cd backend && ./mvnw test

  frontend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run frontend tests
        run: cd frontend && npm ci && npm test

  nlp-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run NLP tests
        run: cd nlp-service && pip install -r requirements.txt && pytest

  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Lint backend
        run: cd backend && ./mvnw checkstyle:check
      - name: Lint frontend
        run: cd frontend && npm run lint
```

---

## Git Workflow

### Branch Strategy

```
master          <- release branch (protected — no direct push)
  ├── hotfix/*    <- critical bug fix (source: master, target: master)
  └── dev         <- integration branch (protected — no direct push)
        ├── feature/*   <- new functionality (→ dev)
        ├── fix/*       <- bug fix (→ dev)
        ├── docs/*      <- documentation (→ dev)
        ├── refactor/*  <- code restructure (→ dev)
        ├── chore/*     <- tooling / CI / deps (→ dev)
        └── release/*   <- release candidate (→ master)
```

### Branch Naming Convention

All branch names use **kebab-case** (`lowercase-with-hyphens`).

| Prefix | Purpose | Example |
|--------|---------|---------|
| `feature/` | New functionality | `feature/nlp-sentiment`, `feature/add-holding-form` |
| `fix/` | Bug fixes | `fix/price-cache-timeout`, `fix/sentiment-score-range` |
| `hotfix/` | Critical production bug (branched from `master`) | `hotfix/crash-on-login`, `hotfix/api-key-expired` |
| `release/` | Release candidate (branched from `dev`, targets `master`) | `release/v0.1.0`, `release/v1.0.0` |
| `docs/` | Documentation only | `docs/swagger-descriptions`, `docs/setup-guide` |
| `refactor/` | Code restructuring (no feature change) | `refactor/impact-correlator`, `refactor/extract-common-charts` |
| `chore/` | Tooling, CI, dependencies | `chore/update-docker-compose`, `chore/add-pre-commit-hooks` |

### Commit Message Convention ([Conventional Commits](https://www.conventionalcommits.org/))

```
<type>: <short description>

feat: add Finnhub company-news endpoint with caching
fix: resolve NPE when price data is empty for ticker
docs: document impact correlator algorithm
refactor: extract price normalisation into shared util
test: add edge cases for sentiment score mapping
chore: update Docker Compose to PostgreSQL 16
```

### Rules

#### Branch Protection

| Rule | `master` | `dev` |
|------|----------|-------|
| Direct push | ❌ Forbidden | ❌ Forbidden |
| Require Pull Request | ✅ | ✅ |
| Require review (≥1) | ✅ | ✅ |
| Require CI pass | ✅ | ✅ |
| Require conversation resolution | ✅ | ✅ |

#### Everyday Workflow

1. **Start** — pull latest `dev`, create a feature branch from `dev`:
   ```bash
   git checkout dev; git pull origin dev
   git checkout -b feature/my-feature
   ```
2. **Commit** — use Conventional Commits; commit often with meaningful messages
3. **Stay in sync** — periodically merge `dev` back into your feature branch to avoid large conflicts:
   ```bash
   git checkout dev; git pull origin dev
   git checkout feature/my-feature; git merge dev
   ```
4. **Push & Open PR** — push your branch, open a PR targeting `dev`, fill in the PR template
5. **Review** — at least one team member must review and approve; CI must pass (tests + lint)
6. **Merge** — use **Merge Commit** (not squash, not rebase) to preserve full branch history when merging into `dev`
7. **Clean up** — delete the feature branch after merge (GitHub can do this automatically; keep the checkbox checked)
8. **Monitor dev CI** — after your PR merges, check that `dev` CI still passes. If `dev` CI goes red, **stop all new feature work** and create a `fix/*` branch immediately (see [Dev CI Failure](#dev-ci-failure) below)

#### Stale Branch Cleanup

- **Draft PRs** — if a draft PR has no activity for **1 week**, the author must either push updates or close it
- **Abandoned branches** — stale branches (no commits for **2+ weeks**, no open PR) are deleted during the weekly sync
- **Weekly check-in** — every Friday, the team reviews all open branches and PRs; any branch not actively being worked on is reassigned or deleted

#### Merge Conflict Resolution

1. **PR author is responsible** for resolving conflicts on their own PR
2. Before resolving, pull latest `dev` and merge locally:
   ```bash
   git checkout dev; git pull origin dev
   git checkout feature/my-feature; git merge dev
   # Resolve conflicts in your editor, then:
   git add .; git commit -m "chore: resolve merge conflicts with dev"
   git push
   ```
3. **If the conflict is unclear** (e.g., another teammate changed the same logic) — ping them on the team channel before resolving
4. **GitHub's "Resolve conflicts" button** is acceptable for trivial conflicts (whitespace, imports); complex ones must be resolved locally and re-reviewed
5. A PR with unresolved merge conflicts must **never** be merged

#### Dev CI Failure

If `dev` CI fails after a merge (possible when two PRs pass individually but conflict post-merge):

1. **Stop** — do not branch from a broken `dev`
2. A **team lead** (or whoever notices first) creates a `fix/ci-dev-<issue>` branch
3. This fix takes priority over all feature work
4. Once fixed and merged back to `dev`, feature work resumes

#### Hotfix Flow (Critical Bug on `master`)

```
master ← hotfix/<description>  (source: master, target: master)
         then merge master → dev immediately after
```

1. Branch from `master`: `git checkout master; git checkout -b hotfix/crash-on-login`
2. Fix + commit using Conventional Commits (`fix: ...`)
3. Open PR targeting **`master`** (not dev)
4. At least one reviewer approves + CI passes
5. Merge Commit into `master`
6. Tag immediately: `git tag -a vX.Y.Z -m "Hotfix: <description>"` — increment **patch** version (`v0.1.0` → `v0.1.1`)
7. **Immediately** merge `master` back into `dev`:
   ```bash
   git checkout dev; git merge master; git push origin dev
   ```

#### Reverting a Bad Merge

If a merged PR introduces a bug discovered after merge:

1. **Preferred**: create a `fix/*` branch off `dev`, fix the bug, follow normal PR flow
2. **If immediate rollback is needed**: use `git revert` (never `git reset --hard` on shared branches):
   ```bash
   git checkout dev; git pull origin dev
   git revert -m 1 <merge-commit-hash> -m "revert: <what and why>"
   git push origin dev
   ```
3. The original PR author investigates root cause and re-submits a corrected PR

#### Release Flow (`dev` → `master`)

> **Release branch protection:** `release/*` branches follow the same rules as `dev` — no direct push, PR required, review required.

1. When `dev` is stable and ready for release:
   ```bash
   git checkout dev; git pull origin dev
   git checkout -b release/vX.Y.Z
   ```
2. Open a PR from `release/vX.Y.Z` → `master`
3. All team members review
4. Merge Commit into `master`
5. Tag the merge commit on `master`:
   ```bash
   git checkout master; git pull origin master
   git tag -a vX.Y.Z -m "Release vX.Y.Z: <brief summary>"
   git push origin vX.Y.Z
   ```
6. **The release PR author** is responsible for merging `master` back into `dev` immediately after tagging:
   ```bash
   git checkout dev; git merge master; git push origin dev
   ```
   Verify `dev` CI passes after the back-merge. If conflicts arise during back-merge, the release manager resolves them with input from affected authors.
7. Delete the `release/*` branch after the back-merge succeeds

#### Tag Naming

| Stage | Tag | When |
|-------|-----|------|
| MVP | `v0.1.0` | Core CRUD working (Week 2) |
| Iteration | `v0.2.0`, `v0.3.0`... | Each major milestone |
| Final | `v1.0.0` | After final presentation |

### Repository Access

- **Single repository** — one GitHub repo for all project work (per project specification)
- **Visibility** — repo must be public **or** shared with instructors (`helppo2`, `tuistmessiah`) and all team members
- **Team access** — ensure every team member can push and create PRs before Week 1 ends

### Repository Configuration Files

The following config files are maintained in the repo root. They are **not** repeated in this document to avoid staleness — the file is the source of truth:

| File | Purpose |
|------|---------|
| [`.gitattributes`](../../.gitattributes) | Line ending normalisation (LF for code, CRLF for PS scripts, binary for images) |
| [`.gitignore`](../../.gitignore) | OS files, editor config, `node_modules/`, `.env`, build output, secrets |

### PR Template

A self-contained PR template is provided at [`.github/pull_request_template.md`](../../.github/pull_request_template.md) — it carries all instructions an AI agent needs to fill in a complete PR. Every PR must use it.

---

## Project Management

### Tool: Jira

Free for teams up to 10 users. Use a **Kanban** project (simpler than Scrum for a 6-week timeline).

**Board Columns:**

```
Backlog          To Do           In Progress      Review           Done
+----------+    +----------+    +----------+    +----------+    +----------+
| User auth|    | DB schema|    | Finnhub  |    | Portfolio|    | Project  |
| E2E tests|    | design   |    | integrat.|    | CRUD PR  |    | skeleton |
| CI/CD    |    | NLP model|    | Impact   |    | Frontend |    | GitHub   |
| ...      |    | training |    | correlat.|    | dashboard|    | repo     |
+----------+    +----------+    +----------+    +----------+    +----------+
```

**Issue Types:**

| Type | Use For |
|------|---------|
| Epic | Each week's milestone (Week 1 ~ Week 6) |
| Story | User-facing feature (P0-P3 items) |
| Task | Technical work item (e.g. "Set up Finnhub API client") |
| Bug | Defect found during testing |

**Labels:** `backend`, `frontend`, `nlp`, `devops`, `docs`

**Status Flow:**

```
To Do  →  In Progress  →  In Review  →  Done
                ↕
             Blocked
```

| Status | Meaning | Trigger |
|--------|---------|---------|
| **To Do** | Ready, waiting for someone to pick up | Default on issue creation |
| **In Progress** | Actively being worked on | Assignee drags after claiming |
| **Blocked** | Stuck — waiting on API key / teammate / environment | Anyone, any time |
| **In Review** | PR opened, awaiting teammate review | Dragged when PR is created |
| **Done** | Merged into `dev` | Dragged after PR merge |

**Board Views:**

| View | Purpose | Who |
|------|---------|-----|
| **Kanban Board** | Daily drag-and-drop, see progress at a glance | Everyone |
| **By Assignee** | See each person's current workload | Individual |
| **By Epic** | Check weekly alignment — are all Week N items on track? | Team lead / standup |
| **Backlog** | Weekly grooming, prioritize next week's stories | Everyone |

### Suggested Task Breakdown (Minimal MVP)

| Week | Tasks | Deliverable | Depends on |
|------|-------|-------------|------------|
| **Week 1** | Project skeleton, GitHub repo, DB schema, Jira setup | Runnable app with DB connection | — |
| **Week 2** | Portfolio + Holdings CRUD (backend + frontend) | Can add/view/remove holdings | Week 1 |
| **Week 3** | Finnhub integration: news fetch + price polling + caching | Data flowing into DB | Week 2 |
| **Week 4** | NLP sentiment service up, Impact Correlator algorithm | Impact events generated | Week 3 |
| **Week 5** | Frontend: Impact Feed, Side-by-Side view, Dashboard charts | Core UI complete | Week 4 |
| **Week 6** | Polish, tests, Swagger docs, Docker, presentation prep | Production-ready demo | Week 5 |

### Ongoing Practices

- **Instructor check-ins** — instructors will drop in regularly to see progress. Keep a running list of questions to ask them when they visit.
- **Design + Build in parallel** — some team members can work on the design of a more fully-featured application while others build small working pieces as demos.
- **Pair programming** — try pair programming where it makes sense; it can be very effective for complex logic (e.g. impact correlator algorithm).

---

## Presentation Plan

> 15 minutes + 5 minutes Q&A — **Tell a story!** Your presentation should have a beginning, a middle, and an end.

### Presentation Rules (Hard Requirements)

| # | Rule | Detail |
|---|------|--------|
| 1 | **Everyone speaks** | Every team member must present at least one section — no silent members |
| 2 | **Cameras on** | Keep your cameras on throughout the entire presentation |
| 3 | **Ask other teams questions** | After other teams present, you are expected to ask questions — prepare thoughtful ones in advance |

### Flow

| Time | Speaker | Content |
|------|---------|---------|
| 0:00-1:00 | Team Lead | Introduce team; what we've been learning; what we were asked to do; how much time we've had (6 weeks) |
| 1:00-2:00 | Team Lead | How we approached the project — roles, tools, technologies, team name |
| 2:00-3:30 | Backend | High-level architecture (diagram), data model walkthrough — explain our design decisions |
| 3:30-5:00 | NLP Lead | Explain sentiment analysis pipeline, finBERT, why local model over LLM API |
| 5:00-9:00 | **ALL** | **LIVE DEMO — The "Wow" Moment** |
| | | 5:00 — Show portfolio dashboard, everything normal |
| | | 6:00 — Trigger breaking news: "Apple cuts iPhone forecast by 10M units" |
| | | 6:30 — Sentiment instantly shows NEGATIVE (97%) |
| | | 7:00 — Side-by-side: news text vs price chart, -3.38% drop |
| | | 7:30 — Impact card: "STRONG negative correlation" |
| | | 8:00 — Show multiple impact events in the feed |
| 9:00-11:00 | Team | Challenges faced — did we work well together? technical hurdles? mistakes made? what would we do differently? |
| 11:00-13:00 | Team Lead | What we'd do next with more time: multi-language news, real-time WebSocket alerts, LLM-based summarization |
| 13:00-15:00 | **ALL** | Thank you for listening — any questions? |

### Demo Preparation Checklist

| # | Task | Owner |
|---|------|-------|
| 1 | Pre-load 50+ news articles for 3-5 tickers into the database | Backend |
| 2 | Pre-run sentiment analysis on all (so labels appear instantly) | NLP Lead |
| 3 | Pre-calculate impact events (so correlation data is ready) | Backend |
| 4 | Test the "trigger new news" flow end-to-end for the live demo moment | ALL |
| 5 | Record a backup demo video in case of internet outage | Frontend |
| 6 | Prepare fallback demo mode: switch to local-only data if Finnhub is down | Backend |
| 7 | Each team member rehearses their section and knows exactly which buttons to click | ALL |

---

## Notes

1. **User Management:** Per the project specification, a single user can be assumed initially. User authentication is optional and should only be added if time permits after core features are complete.
2. **Start Small:** The first working version should store a minimal data model — a portfolio with just `id`, `ticker`, and `shares`. Enhance incrementally.
3. **External API Resilience:** Every project should demonstrate fallback behavior when an external API is unavailable. Our caching layer in `stock_prices` and `news_articles` serves this purpose — the demo should explicitly show this resilience.
4. **Quality Over Quantity:** A polished 3-ticker demo with clean UI and working NLP beats a buggy 20-ticker system.
5. **Stay Agile:** The single biggest problem teams face is starting with a data model that is too complex. Begin with `Portfolio(id, name)` + `Holding(id, portfolioId, ticker, shares)`. Add sentiment, prices, and impact tables after the core CRUD works.

---

## License

This project is developed as part of the Final Project training program.

---

> **"We don't just show you the news. We show you what the news means for your money."**
