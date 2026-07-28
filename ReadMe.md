# Financial News & Portfolio Impact Service (FNPIS)

> **Project #15 · Group 7** — Aggregates financial news for companies in a portfolio and estimates which holdings are affected by integrating a financial news API and a stock price API. Scores each headline for sentiment with an LLM agent, then reports whether the price actually moved the way the news implied.

[中文版 (Chinese)](./ReadMe.zh-CN.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Design Documents](#design-documents)
3. [Team](#team)
4. [Tech Stack](#tech-stack)
5. [Architecture](#architecture)
6. [Project Structure](#project-structure)
7. [Getting Started](#getting-started)
8. [REST API Documentation](#rest-api-documentation)
9. [Database Schema](#database-schema)
10. [External API Integration](#external-api-integration)
11. [Sentiment Analysis](#sentiment-analysis)
12. [Frontend Pages](#frontend-pages)
13. [Scheduled Jobs](#scheduled-jobs)
14. [Testing](#testing)
15. [CI/CD & Docker](#cicd--docker)
16. [Git Workflow](#git-workflow)
17. [Project Management](#project-management)
18. [Presentation Plan](#presentation-plan)

---

## Overview

### The Problem

Investors hold portfolios of stocks but struggle to connect the dots between breaking financial news and its actual impact on their holdings. A headline about Apple cutting production forecasts should immediately signal a potential price drop — but most investors discover the connection hours too late.

### Our Solution

FNPIS is a full-stack web application that:

1. **Aggregates** financial news for every company in a user's portfolio via the Finnhub API
2. **Analyzes** each headline's sentiment (Positive / Negative / Neutral) with an LLM agent
3. **Correlates** news sentiment with the day's price move, weighted by position size
4. **Visualizes** the impact side-by-side — news on the left, price chart on the right

### What Makes the Output Honest

The system answers two questions separately and never blends them into one score:

| Question | Answer | Derived from |
|----------|--------|--------------|
| What *should* this news have done to the position? | `Direction` — POSITIVE / NEGATIVE / NEUTRAL | Sentiment |
| Did the price *actually* agree? | `Alignment` — CONFIRMED / DIVERGENT / INCONCLUSIVE | Daily return vs. sentiment |

`DIVERGENT` is not a bug. Negative news that the market shrugged off is the interesting
case, and a system that hides it is just telling you what you already assumed.
`INCONCLUSIVE` covers two situations that must not be dressed up as verdicts: the move
was smaller than epsilon and is therefore noise, or the previous close was unavailable
and no return could be computed at all.

### Core Features (Priority Order)

| Priority | Feature | Description | Acceptance Criteria |
|----------|---------|-------------|---------------------|
| P0 | Browse Records | View portfolio holdings, news feed, and impact assessments | User sees all holdings with ticker, shares, and current price; news feed paginates |
| P1 | View Metrics | Charts: portfolio value history, per-holding valuation, the news/price impact view | Impact view renders the price series with the news marker; value history handles the empty case |
| P2 | Add Items | Add holdings, trigger a news refresh | User enters ticker + shares → holding appears → news arrives within one poll cycle |
| P3 | Remove Items | Remove holdings | Removed holding disappears from the portfolio and its valuation |

### Integration Requirement

FNPIS integrates two distinct external data interfaces plus an LLM API:

| API | Purpose | Auth |
|-----|---------|------|
| [Finnhub](https://finnhub.io/) company-news | Financial news per ticker (1 year history) | Free API key — dedicated account |
| [Finnhub](https://finnhub.io/) quote + candles | Current quote and historical daily bars | Free API key — **separate** account |
| LLM API | Headline sentiment classification | API key |

News and prices deliberately use different keys so one chain exhausting its quota cannot
starve the other. Rate limiters are configured per key, never shared — otherwise the
15-minute news poll saturates the limiter and quote refresh gets rejected alongside it,
which defeats the whole point of two accounts.

Single-vendor dependency is a real risk, and multiple keys do nothing about it: if
Finnhub itself goes down, both chains go down together. The `PriceProvider` interface is
what actually contains that risk — adding another vendor's implementation touches no
business logic and no table.

---

## Design Documents

**The architecture document is authoritative for technical decisions.** This ReadMe is an
orientation for humans; where the two disagree, the architecture document wins.

| Document | Covers |
|----------|--------|
| [`docs/项目15-①需求文档.md`](docs/项目15-①需求文档.md) | Requirements A–G, sentiment rules, impact formulas with worked examples, edge cases EC-01–EC-24 |
| [`docs/项目15-②架构设计.md`](docs/项目15-②架构设计.md) | Layering, key decisions, data model, project structure |
| [`docs/项目15-③API契约.md`](docs/项目15-③API契约.md) | Endpoints, pagination, error format. Superseded by Swagger once the backend is implemented |

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

> Role assignments (Backend Lead / Frontend Lead / additional roles) to be decided by the team in Week 1.

Backend work is split three ways, and the split determines what unblocks what:

| Scope | Owner | Notes |
|-------|-------|-------|
| Portfolio + holdings CRUD | one developer | **Also owns the Flyway scripts and every `@Entity`.** Nobody else touches the schema |
| News aggregation + market data | one developer | — |
| Sentiment + impact assessment | one developer | Blocked until the entities exist |

Everyone writes their own `repository/` interfaces against the shared entities. Single
schema ownership is deliberate: concurrent migration edits are the fastest way to make
everyone's checkout fail to start.

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
| **Frontend** | SPA — framework is the frontend dev's choice | Does not affect the backend, data model, or API contract |
| **Charts** | Chart.js 4 + annotation plugin | The annotation plugin draws the news marker line on the price chart; this is the one frontend constraint |
| **Database** | MySQL 8 + Flyway | Versioned SQL migrations; utf8mb4 throughout |
| **Sentiment** | LLM agent, single engine | See [Sentiment Analysis](#sentiment-analysis) |
| **External APIs** | Finnhub — separate key for news and prices | Per-purpose quota isolation |
| **HTTP client** | RestClient (Spring 6.1+) | — |
| **Resilience** | Resilience4j | Rate limiting, retry, circuit breaking |
| **Local cache** | Caffeine + Spring Cache | Wraps outbound provider calls |
| **Scheduling** | Spring `@Scheduled` | News poll, quote refresh, closing snapshot, impact recompute |
| **API Docs** | springdoc-openapi | Generated from annotations, never hand-written YAML |
| **CI/CD** | GitHub Actions | Lint → type-check → test → build on every PR |
| **Container** | Docker + Docker Compose | One-command local setup; portable deployment |
| **Version Control** | Git + GitHub | Feature branches, PR reviews |
| **Project Mgmt** | [Jira](https://therain2026.atlassian.net/jira/software/projects/FNPIS/boards/3) | Task tracking, Kanban board |

**No performance SLO.** End-to-end latency is dominated by the Finnhub poll interval, so
publishing a response-time target would be self-deception. The real quality bar is the
success criteria in the requirements document.

### Implementation Conventions

Three conventions that cause problems if any one person ignores them:

**Money and share counts are `BigDecimal`, never `double` or `float`.** Floating-point
arithmetic gives `0.1 + 0.2 = 0.30000000000000004`, and an instructor checking the demo
by hand will find the discrepancy. MySQL side is `DECIMAL`. Checkstyle fails the build on
a `double` named like a monetary value.

**Monetary JSON fields are serialized as strings.** JavaScript's `Number` is a double, so
large values lose precision in transit. The frontend displays them and does no arithmetic.

**Timestamps are UTC `Instant`, converted at the display layer.** Finnhub returns Unix
timestamps, US markets trade in US Eastern, and users read the page in another timezone
again — three timezones in play means an unconverted value silently lands on the wrong
attribution date. The JDBC URL sets `connectionTimeZone=UTC` explicitly.

**Enums are stored as strings** via `@Enumerated(EnumType.STRING)`. Storing ordinals means
inserting a value into the middle of an enum silently corrupts every historical row.

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│  Frontend SPA                                    │
│   Portfolio / Holdings / News feed / Impact view │
└──────────────────┬──────────────────────────────┘
                   │ REST + JSON
┌──────────────────▼──────────────────────────────┐
│  API layer                                       │
│   /api/v1/**     internal (frontend)             │
│   /public/v1/**  external read-only (API key)    │
│   /swagger-ui    documentation                   │
└──────────────────┬──────────────────────────────┘
┌──────────────────▼──────────────────────────────┐
│  Service layer — all business logic lives here   │
│   PortfolioService   holdings CRUD, valuation    │
│   NewsService        aggregate, dedupe, link     │
│   SentimentService   LLM agent                   │
│   ImpactService      impact assessment ★ core    │
│   MarketDataService  quotes and history          │
└────────┬──────────────────────────┬──────────────┘
         │                          │
┌────────▼───────────────┐  ┌───────▼─────────────┐
│  Integration layer     │  │  Scheduler layer    │
│   NewsProvider         │  │   News poll         │
│   PriceProvider        │  │   Quote refresh     │
│   SentimentEngine      │  │   Closing snapshot  │
│                        │  │   Impact recompute  │
│  Impls: Finnhub, Mock  │  └───────┬─────────────┘
│  Caffeine cache        │          │
│  Resilience4j          │          │
└────────┬───────────────┘          │
┌────────▼──────────────────────────▼─────────────┐
│  Persistence — MySQL + versioned migrations      │
└─────────────────────────────────────────────────┘
```

The system has **two entry points: HTTP and the clock.** That is why there are five
layers rather than the three of a plain CRUD app.

| Layer | May do | Must never do |
|-------|--------|---------------|
| API | Validation, DTO conversion, auth | Business logic |
| Service | All business rules, transactions | Call third-party HTTP directly |
| Integration | Call third parties, cache, retry, map formats | Business rules |
| Scheduler | Trigger tasks | Implement logic — delegate to services |
| Persistence | Read and write | Compute |

> The rule most likely to get broken is **"services must not call third parties
> directly."** One HTTP call inlined into a service to save time forfeits both the
> swappable-provider and the graceful-degradation properties at once. Review watches for it.

### Key Decisions

**Three Provider interfaces.** `NewsProvider`, `PriceProvider`, and `SentimentEngine`.
Services depend on the interface only. Finnhub DTOs must not appear in any `service/`
method signature — once a vendor's field names leak into business code, changing vendor
means changing business logic. Review checks the import direction of `integration/finnhub/`.

**Reads never call a provider inline.** Every external fetch is landed by a scheduled job
first; read endpoints query the database. Responses carry `asOf` (when the data was
captured) and `stale` (upstream currently unavailable or rate limited), so a dead upstream
degrades to older data instead of an error page. Both fields must be displayed.

**Impact results are persisted, not computed per request.** The read path serves rows.

**News is fetched per ticker, not from a global feed.** The company-news endpoint makes
the article-to-symbol association exact by construction, so no text matching is needed.

**Daily bars are the only price source for impact.** Comparing today's close against the
previous close keeps the calculation reproducible; intraday quotes are for display.

---

## Project Structure

Packages are organised by layer, matching the architecture document.

```
FNPIS/
├── backend/
│   ├── src/main/java/com/fnpis/
│   │   ├── api/                  # @RestController + DTO
│   │   │   ├── internal/         #   /api/v1/**
│   │   │   └── pub/              #   /public/v1/**  (external read-only)
│   │   ├── service/              # Business logic (@Service)
│   │   ├── integration/          # Anti-corruption layer
│   │   │   ├── NewsProvider.java, PriceProvider.java, SentimentEngine.java
│   │   │   ├── finnhub/          #   Finnhub impls, DTOs, mapping
│   │   │   ├── mock/             #   Offline impls for tests and demo fallback
│   │   │   └── sentiment/        #   Agent + Stub engines, output validation
│   │   ├── scheduler/            # @Scheduled tasks
│   │   ├── domain/               # @Entity + enums
│   │   ├── repository/           # Spring Data JPA interfaces
│   │   ├── common/               # Paging envelope, Freshness, RFC 7807 error types
│   │   └── config/               # Cache, Jackson, OpenAPI, Resilience4j
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── db/migration/         # Flyway scripts
│   │   └── prompts/              # sentiment-prompt.txt (versioned)
│   ├── src/test/java/...
│   ├── checkstyle.xml
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
├── docs/                            # Requirements, architecture, API contract
├── docker-compose.yml               # mysql + backend (+ frontend once it exists)
├── .env.example                     # committed
├── .env                             # gitignored
├── .github/
│   ├── workflows/ci.yml
│   └── pull_request_template.md
├── ReadMe.md
└── ReadMe.zh-CN.md
```

### Current Status

The backend skeleton is in place: build, config, shared response types, domain enums,
Docker, and CI. `api/`, `service/`, `integration/`, `scheduler/`, and `repository/` are
**not yet created** — every one references `@Entity` types in its method signatures, and
the entities are owned by one person who is writing them alongside the Flyway scripts.
Creating those packages before the entities exist would leave the repo non-compiling for
the whole team.

`common/` is an addition rather than part of the original layout: the paging envelope,
freshness type, and error types are imported by every developer, so putting them under
`api/` would make one person's package own types everyone depends on.

---

## Getting Started

### Prerequisites

- **Java 17+** (the Maven wrapper handles Maven itself)
- **Docker & Docker Compose**
- **Two Finnhub API keys** — [register](https://finnhub.io/register); one account for news, one for prices
- **An LLM API key** for sentiment

### Environment Variables

Copy the template and fill it in. `.env` is gitignored and must stay that way.

```bash
cp .env.example .env
```

`.env.example` is the authoritative list of variables; it is kept in sync by CI. The
shape:

| Variable | Notes |
|----------|-------|
| `DB_HOST`, `DB_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD` | Database |
| `FINNHUB_KEY_NEWS` | News polling only |
| `FINNHUB_KEY_PRICE` | Quotes and daily bars only |
| `LLM_API_KEY`, `LLM_BASE_URL`, `LLM_MODEL` | Sentiment engine |
| `SERVER_PORT` | App |
| `PROVIDER_NEWS`, `PROVIDER_PRICE`, `PROVIDER_SENTIMENT` | `finnhub`/`mock`, `finnhub`/`mock`, `agent`/`stub` |

**No secret has a default value anywhere.** A default in `application.yml` carries the
same exposure as committing the key, so the app fails fast at startup naming the missing
variable instead of collecting 401s minutes later. Keys are redacted from logs.

> The LLM key deserves the most care: a leaked Finnhub key costs you a quota, a leaked
> LLM key bills real money.

Setting all three `PROVIDER_*` switches to the offline implementation runs the whole
system with no external calls. This is what E2E tests use and the demo-day fallback.

### Quick Start (Docker)

```bash
git clone https://github.com/Neueda-Learning/CLOSEAI_financial-news-and-portfolio-impact-service.git
cd CLOSEAI_financial-news-and-portfolio-impact-service

cp .env.example .env
# Edit .env with your API keys

docker compose up -d

# Backend API docs:  http://localhost:8080/swagger-ui.html
# Health:            http://localhost:8080/actuator/health
```

### Manual Start (Development)

```bash
# Terminal 1 — database only
docker compose up -d mysql

# Terminal 2 — backend
cd backend
./mvnw spring-boot:run               # :8080

# Terminal 3 — frontend (once it exists)
cd frontend
npm install
npm run dev
```

### Before You Push

```bash
cd backend
./mvnw checkstyle:check              # style gate
./mvnw clean verify                  # compile + tests
```

---

## REST API Documentation

Full interactive docs available at `http://localhost:8080/swagger-ui.html` after startup.

> **Bonus Objective:** Expose the API so instructors can query system information during the final presentation (e.g. `GET /api/portfolios` to verify holdings, `GET /api/impact?portfolioId=1` to inspect impact events). Swagger UI serves as both documentation and a live inspection tool for this purpose.

The full endpoint list, request/response shapes, and error catalogue live in
[`docs/项目15-③API契约.md`](docs/项目15-③API契约.md). They are not duplicated here —
a second copy is a copy that goes stale.

### Base URLs

```
/api/v1/**        internal, used by the frontend
/public/v1/**     external read-only, API key required
```

### Conventions

**Pagination** is 1-based for clients. `size` defaults to 20 and is capped at 100;
anything larger is clamped rather than rejected.

**Freshness.** Most responses carry `asOf` (when the underlying data was captured) and
`stale` (upstream currently unavailable or rate limited). The frontend must display both.

**Monetary values are JSON strings**, not numbers.

**Errors** follow RFC 7807 (`application/problem+json`). Clients branch on the stable
`code` field, never on the human-readable `detail`:

```json
{
  "type": "https://fnpis.local/errors/security-not-found",
  "title": "Security not found",
  "status": 404,
  "detail": "No security with symbol 'XYZQ'",
  "code": "SECURITY_NOT_FOUND",
  "instance": "/api/v1/securities/XYZQ"
}
```

Validation failures append an `errors` array; the rest of the skeleton stays constant.

### The Endpoint That Matters

`GET /api/v1/news/{id}/impact-view?portfolioId=1` returns everything the side-by-side
demo view needs in one request: the article with its sentiment, the per-symbol impact
rows (weight, price change, expected vs. observed, value impact, direction, alignment),
and the price series with the news timestamp marked. The frontend stitches nothing
together.

`GET /api/v1/portfolios/{id}/valuation-history` will legitimately return an empty
`points` array on a fresh install — snapshots accumulate one per trading day. The
frontend must render "collecting data" rather than an empty chart or an error.

`POST /api/v1/news/refresh` is the demo's manual trigger. Calling it while a refresh is
already running returns **409**, not silence — during a live demo, silence is worse than
an error. Click it twice and the second call should report `inserted: 0`, which is the
dedupe acceptance check.

---

## Database Schema

Schema is owned by **one person** and defined by versioned Flyway scripts in
`backend/src/main/resources/db/migration/`. Column-level detail lives in the
architecture document; this is the map.

```
portfolio 1──n holding n──1 security
                                │
news_article n──n article_security_link
     │
     └─1─1 sentiment_score

security 1──n price_quote    (latest quote, one row per symbol)
security 1──n price_bar      (historical daily bars)

impact_assessment ──► news_article + security + portfolio
```

| Table | Primary key | Key constraint |
|-------|-------------|----------------|
| `portfolio` | `id` | — |
| `holding` | `id` | FK to portfolio, **UNIQUE** (portfolio_id, symbol) |
| `security` | `symbol` | Natural key — no surrogate id |
| `news_article` | `id` | **UNIQUE (external_id)** — dedupe depends entirely on this |
| `article_security_link` | (article_id, symbol) | Composite key, inherently duplicate-proof |
| `sentiment_score` | `id` | **UNIQUE (article_id)** — single engine, one score per article |
| `price_quote` | `symbol` | Latest row only, written by upsert |
| `price_bar` | (symbol, trade_date) | Composite |
| `impact_assessment` | `id` | **UNIQUE** (article_id, symbol, portfolio_id, attribution_date) + index on (portfolio_id, attribution_date) |
| `portfolio_valuation_snapshot` | (portfolio_id, snapshot_date) | FK to portfolio, composite PK |

### Three Modelling Points

**`news_article.external_id` must be unique.** Deduplication relies on it completely.
Without the constraint, every scheduled run re-inserts the same articles.

**News-to-symbol is many-to-many and needs its own table.** One article can move several
stocks ("chip stocks rally"), and one stock has many articles. Putting a `symbol` column
on `news_article` is the easiest mistake to make here, and undoing it later means
rewriting the migration and every query that touches it. The link table also carries how
the association was made, which a bare `@ManyToMany` join table cannot store.

**`sentiment_score.article_id` is unique**, which usefully also guarantees no article gets
analyzed twice — reruns cost no extra LLM calls. `model_version` is retained even without
multi-engine comparison: after a model or prompt change it is the only way to tell which
version produced a given verdict.

### Migration Rules

- **Never edit a committed script.** Flyway stores a checksum, so a modified file makes
  every other checkout fail at startup. Add a new version instead.
- V1–V6 are reserved. Claim V7+ and tell the team.
- `ddl-auto: validate` — Hibernate never creates or alters tables, it only verifies that
  the entities match what Flyway built. A mismatch fails startup, which is the point.

---

## External API Integration

### Finnhub

| Endpoint | Usage | Key |
|----------|-------|-----|
| `/company-news?symbol=AAPL&from=…&to=…` | Per-ticker news | `FINNHUB_KEY_NEWS` |
| `/quote?symbol=AAPL` | Current quote incl. previous close | `FINNHUB_KEY_PRICE` |
| `/stock/candle?symbol=AAPL&resolution=D&…` | Historical daily bars | `FINNHUB_KEY_PRICE` |

Which key gets used is an internal detail of the integration layer. `FinnhubNewsProvider`
reads only the news key, `FinnhubPriceProvider` only the price key, and no business code
knows how many keys exist.

**Rate limiting.** One Resilience4j limiter instance per key. Sharing one would let the
news poll saturate the limiter and take quote refresh down with it, which would make the
two accounts pointless. A 429 on the news key degrades only the news chain — prices keep
working and only the news panel shows a stale marker.

**Degradation.** Reads are served from the database, so a rate-limited or dead upstream
produces older data flagged `stale: true`, not an error. `UPSTREAM_UNAVAILABLE` is only
returned for a symbol that has never been fetched successfully, so there is nothing to
serve.

**Keys never reach the logs.** Request URLs are logged with `token=***`.

### Known Risks

**Does the free tier expose historical daily bars?** The impact engine reads
today's-close vs. previous-close from `price_bar`. If that endpoint is unavailable, the
fallback is to build the bar table from daily closing snapshots — which needs **several
days of lead time** before there is enough data to chart. This is the highest-priority
day-one question, and extra API keys cannot solve it: a quota problem and a permission
problem are different things.

**Is rate limiting per key or per IP?** If per IP, separate accounts on one machine buy
nothing and the multi-key design collapses back to a single key with a longer poll
interval.

**Does the free tier allow one person holding several accounts?** Most vendors prohibit
it outright, and the worst case is every account banned the day before the demo. Keep the
Mock Provider switchable regardless — "hoping the keys still work" is not a fallback plan.

---

## Sentiment Analysis

### Approach: LLM Agent, Single Engine

A single LLM-backed `SentimentEngine` classifies each headline as POSITIVE, NEGATIVE, or
NEUTRAL with a score and confidence. There is no local model and no Python service.

Multi-engine comparison was considered and dropped: two engines double the failure modes
and the demo narration for a comparison nobody asked for.

### What Has to Be Handled

An LLM is a network call that returns text, so the engine treats every response as
untrusted and validates before persisting:

| Failure | Handling |
|---------|----------|
| Illegal label | Reject |
| Score out of range | Reject |
| Response is not JSON | Reject |
| Score disagrees with its own label (`NEGATIVE` with `+0.8`) | Reject |

Each of these has a test. Temperature is 0 and the prompt is versioned in
`resources/prompts/` — `model_version` on each row records which prompt produced it, so
after a prompt change you can still tell where an old verdict came from.

`StubSentimentEngine` returns fixed results with no network call. CI and E2E tests use it,
which is why CI needs no real API keys.

The unique constraint on `sentiment_score.article_id` means an article is never analyzed
twice, so a rerun costs nothing.

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
|  | POSITIVE  12    NEGATIVE  5    NEUTRAL  3            | |
|  +-----------------------------------------------------+ |
|                                                          |
|  Data as of 2026-07-27 15:42 UTC                         |
|                                                          |
|  [+ Add Holding]  [View Impact Feed ->]                   |
+----------------------------------------------------------+
```

Every page that shows external data shows `asOf`, and surfaces a marker when `stale` is
true. This is a product feature, not a debug affordance — it is what lets the app stay
readable when an upstream is down.

### Page 2: News & Impact Feed (Core Demo Page)

```
+--------------------------------------------------------------+
|  News & Impact Feed                    Last 7 days   [Refresh]|
|                                                              |
|  +-- Impact Assessments -----------------------------------+ |
|  |                                                        | |
|  |  AAPL   -3.38%    NEGATIVE    CONFIRMED                | |
|  |  +----------------------------------------------------+ | |
|  |  | "Apple cuts iPhone production forecast             | | |
|  |  |  by 10M units" — Reuters                           | | |
|  |  |  Sentiment: NEGATIVE (conf 0.97)                   | | |
|  |  |  Weight 21.3%  |  Value impact -$1,320.00          | | |
|  |  +----------------------------------------------------+ | |
|  |                                                        | |
|  |  TSLA   +0.41%    NEGATIVE    DIVERGENT                | |
|  |  +----------------------------------------------------+ | |
|  |  | "Tesla recalls 12,000 vehicles" — Reuters          | | |
|  |  |  Sentiment: NEGATIVE (conf 0.81)                   | | |
|  |  |  The market shrugged it off. This is the           | | |
|  |  |  interesting case, not an error.                   | | |
|  |  +----------------------------------------------------+ | |
|  +--------------------------------------------------------+ |
|                                                              |
|  Data as of 2026-07-27 16:05 UTC                             |
+--------------------------------------------------------------+
```

### Page 3: Side-by-Side Comparison (Presentation Hook)

Served by one call to `/news/{id}/impact-view`. The vertical line marking the news
timestamp is what the Chart.js annotation plugin is for.

```
+--------------------------------+--------------------------------+
|  News Analysis                 |  Price (AAPL)                  |
|                                |                                |
|  NEGATIVE   conf 0.97          |  $196 -|                       |
|                                |  $194 -|    \                  |
|  "Apple cuts iPhone            |  $192 -|     \                 |
|   production forecast          |  $190 -|      \_____           |
|   by 10M units"                |  $188 -|            \___       |
|                                |  $186 -|                \___   |
|  Source: Reuters               |        |----|----|----|----|   |
|  Published: 2026-07-27         |      9:00 10:00 11:00 12:00  |
|  13:30 UTC                     |           :                    |
|                                |      news published            |
|  Weight:        21.3%          |                                |
|  Price change:  -3.38%         |  Previous close: $195.30      |
|  Expected:      -0.19          |  Attribution:    2026-07-27   |
|  Observed:      -0.72          |                                |
|  Value impact:  -$1,320.00     |  Data as of 15:42 UTC         |
|  Direction:     NEGATIVE       |  stale: false                  |
|  Alignment:     CONFIRMED      |                                |
+--------------------------------+--------------------------------+
```

### Page 4: Portfolio Value History

A line chart from `/portfolios/{id}/valuation-history`. On a fresh install `points` is
empty, since snapshots accumulate one per trading day — render "collecting data", not an
empty chart and not an error.

---

## Scheduled Jobs

Nothing the frontend does triggers an external API call. Every external fetch happens
here, ahead of time, and lands in the database. That single property is what shapes the
whole architecture.

| Job | Cadence | Description |
|-----|---------|-------------|
| News poll | 15 min | Incremental fetch per watchlist symbol, deduped on `external_id` |
| Sentiment | After news lands | Only articles with no `sentiment_score` row |
| Quote refresh | 1 min, market hours only | Upserts `price_quote` |
| Closing snapshot | Daily after close | Writes `price_bar` + portfolio valuation snapshot |
| Impact recompute | After the snapshot | Generates the day's `impact_assessment` rows |

```
news poll ──► sentiment ──┐
                          ├──► impact recompute
closing snapshot ─────────┘
```

Impact recompute must wait for both branches, or it runs on incomplete data.

**Do not skip the closing snapshot.** It is the only source for the portfolio value chart,
and re-querying history from the API repeatedly does not fit in the free quota. Worth
knowing: portfolio value cannot be back-derived later. `price_bar` keeps historical
prices, but historical *holdings* are not kept — today's positions multiplied by an old
close answers a different question. Each night that passes without the snapshot running is
a data point gone for good.

### Concurrency

Overlap is inevitable when a free API is slow, so it is handled explicitly rather than
hoped away.

`@Scheduled` with `fixedDelay` (not `fixedRate`) means a job can never overlap itself —
the next run is counted from the end of the previous one. Manual triggers arrive on an
HTTP thread and still need an explicit lock; failing to acquire it returns 409.

**Skip, never queue.** The news poll is idempotent thanks to `external_id`, so skipping a
round costs at most 15 minutes of freshness. Queueing piles up requests that all fire when
the upstream recovers, tripping the rate limiter — strictly worse.

---

## Testing

| Level | Tools | What it covers |
|-------|-------|----------------|
| Unit | JUnit 5 + Mockito | Providers mocked. **The impact engine must have unit tests** — the requirements document's three worked examples exist to be used verbatim as cases |
| Integration | `@SpringBootTest` + **Testcontainers MySQL** | Scheduled jobs, dedupe, Flyway scripts against a real database |
| Integration layer | **WireMock** | Field mapping, rate-limit retry, error handling |
| Sentiment validation | JUnit 5 | Malformed LLM responses (see [Sentiment Analysis](#sentiment-analysis)) |
| Frontend | Jest + React Testing Library | Component rendering, mocked API responses |
| E2E | Playwright or Cypress | All-Mock providers, so results are deterministic and network-independent |

**Testcontainers with real MySQL, not H2.** H2's compatibility mode differs from MySQL on
`DECIMAL` precision, date functions, and unique-index length limits. A Flyway script that
passes on H2 can still fail on real MySQL, which makes the test worse than useless — it
reports safety it did not verify.

The requirements document lists 24 edge cases (EC-01–EC-24) written from real bug
scenarios; aim for one test each. Three are division-by-zero traps that must be covered:
zero cost basis, missing previous close, and zero total value when computing weight.

### End-to-End Scenarios

```
1. Add a holding            -> appears in the portfolio
2. Trigger news refresh     -> feed populates; second click inserts 0 (dedupe)
3. Sentiment labels         -> appear on articles
4. Trigger impact recompute -> assessments generated
5. Open the impact view     -> chart renders with the news marker line
6. Remove a holding         -> cleanup verified
```

---

## CI/CD & Docker

### CI Pipeline (`.github/workflows/ci.yml`)

Jobs activate conditionally — a job whose directory does not exist yet is skipped rather
than failed.

| Job | Active when | What it runs |
|-----|-------------|--------------|
| `commitlint` | Always | Conventional Commits, checked across every commit in the PR |
| `backend-lint` | `backend/` exists | Checkstyle |
| `backend-type-check` | `backend/` exists | `mvn compile` |
| `backend-test` | `backend/` exists | JUnit 5 |
| `frontend-lint` | `frontend/` exists | ESLint |
| `frontend-type-check` | `frontend/tsconfig.json` exists | `tsc --noEmit` |
| `frontend-test` | `frontend/` exists | Jest |
| `build` | Always | Validate compose config + package the backend jar |

`build` seeds `.env` from `.env.example` first: the compose file uses required-variable
syntax, so interpolation fails outright without it. That also turns the job into a sync
check on the template — adding a required variable without listing it in `.env.example`
turns this red.

**CI needs no real API keys.** Finnhub is stubbed with WireMock and sentiment uses
`StubSentimentEngine`. Testcontainers works on the GitHub ubuntu runner as-is, since
Docker is preinstalled.

> **Critical rule:** CI must trigger on **both** PR and push to `dev`/`master`. The push trigger catches post-merge failures when two PRs pass individually but break `dev` when combined (see [Dev CI Failure](#dev-ci-failure)).

### Docker

Three services: `mysql`, `backend`, and `frontend` (commented out until it has a
Dockerfile). The file itself is the source of truth — see
[`docker-compose.yml`](docker-compose.yml). Four things in it are load-bearing:

**utf8mb4 is set explicitly on the server.** Headlines carry emoji and symbols that plain
`utf8` cannot store, and the insert fails rather than degrading.

**The MySQL healthcheck is not optional.** Flyway connects the instant the backend starts,
and the container accepts connections several seconds after the process launches. Without
`depends_on: condition: service_healthy` the backend dies on boot.

**Secrets use required-variable syntax** (`${MYSQL_PASSWORD:?…}`), so a missing value
fails immediately with a message naming the variable instead of silently starting with a
blank password.

**The backend image is multi-stage.** `pom.xml` is copied and dependencies resolved before
the sources, so editing a Java file does not re-download everything. The runtime layer
carries a JRE and no Maven, runs as a non-root user, and sizes its heap from the
container's memory limit rather than the host's.

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
| `feature/` | New functionality | `feature/sentiment-engine`, `feature/add-holding-form` |
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
chore: pin MySQL to 8.4 in Docker Compose
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

Free for teams up to 10 users. Use a **Kanban** project (simpler than Scrum for a 5-day timeline).

**Board Columns:**

```
Backlog          TODO            IN PROCESS      IN REVIEW        COMPLETED
+----------+    +----------+    +----------+    +----------+    +----------+
| User auth|    | Sentiment|    | Finnhub  |    | Portfolio|    | DB schema|
| E2E tests|    | engine   |    | integrat.|    | CRUD PR  |    | skeleton |
| ...      |    | Impact   |    | Quote    |    | Frontend |    | GitHub   |
|          |    | engine   |    | refresh  |    | dashboard|    | repo     |
+----------+    +----------+    +----------+    +----------+    +----------+
```

**Issue Types:**

| Type | Use For |
|------|---------|
| 长篇故事 (Epic) | Feature module grouping (A–G, Infrastructure) |
| 故事 (Story) | User-facing feature (A1–A7, B1–B5, …, G1–G3) |
| 子任务 (Subtask) | Implementation task, child of a Story |
| Feature | Cross-story technical capability |
| 缺陷 (Bug) | Defect found during testing |

**Labels:** `p0`, `p1`, `p2`, `backend`, `frontend`, `core-logic`, `demo-hook`, `test`, `data`, `api`, `infra`

**Status Flow:**

```
TODO  →  IN PROCESS  →  IN REVIEW  →  COMPLETED
  ↓
BLOCKED   (draggable from IN PROCESS, its own column)
```

| Status | Meaning | Trigger |
|--------|---------|---------|
| **TODO** | Ready, waiting for someone to pick up | Default on issue creation |
| **IN PROCESS** | Actively being worked on | Assignee drags after claiming |
| **BLOCKED** | Stuck — waiting on API key / teammate / environment | Anyone, any time |
| **IN REVIEW** | PR opened, awaiting teammate review | Dragged when PR is created |
| **COMPLETED** | Merged into `dev` | Dragged after PR merge |

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
| **Week 4** | Sentiment engine + impact assessment engine | Impact assessments generated | Week 3 |
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
| 0:00-1:00 | Team Lead | Introduce team; what we've been learning; what we were asked to do; how much time we've had (5 days) |
| 1:00-2:00 | Team Lead | How we approached the project — roles, tools, technologies, team name |
| 2:00-3:30 | Backend | High-level architecture (diagram), data model walkthrough — explain our design decisions |
| 3:30-5:00 | Sentiment owner | Sentiment pipeline: why one LLM engine, how malformed responses are rejected, why `DIVERGENT` is a feature |
| 5:00-9:00 | **ALL** | **LIVE DEMO — The "Wow" Moment** |
| | | 5:00 — Show portfolio dashboard, everything normal |
| | | 6:00 — Trigger breaking news: "Apple cuts iPhone forecast by 10M units" |
| | | 6:30 — Sentiment instantly shows NEGATIVE (97%) |
| | | 7:00 — Side-by-side: news text vs price chart, -3.38% drop |
| | | 7:30 — Impact card: direction NEGATIVE, alignment CONFIRMED, value impact in dollars |
| | | 8:00 — Show a DIVERGENT case and explain why we report it instead of hiding it |
| | | 8:30 — Flip a provider to Mock live: page still reads, `stale: true` appears |
| 9:00-11:00 | Team | Challenges faced — did we work well together? technical hurdles? mistakes made? what would we do differently? |
| 11:00-13:00 | Team Lead | What we'd do next with more time: multi-language news, real-time WebSocket alerts, LLM-based summarization |
| 13:00-15:00 | **ALL** | Thank you for listening — any questions? |

### Demo Preparation Checklist

| # | Task | Owner |
|---|------|-------|
| 1 | Pre-load 50+ news articles for 3-5 tickers into the database | Backend |
| 2 | Pre-run sentiment analysis on all (so labels appear instantly) | Sentiment owner |
| 3 | Pre-compute impact assessments, including at least one DIVERGENT case | Backend |
| 3b | **Start the closing-snapshot job days in advance** — the value chart has no other data source | Backend |
| 4 | Test the "trigger new news" flow end-to-end for the live demo moment | ALL |
| 5 | Record a backup demo video in case of internet outage | Frontend |
| 6 | Prepare fallback demo mode: switch to local-only data if Finnhub is down | Backend |
| 7 | Each team member rehearses their section and knows exactly which buttons to click | ALL |

---

## Notes

1. **User Management:** Per the project specification, a single user can be assumed initially. User authentication is optional and should only be added if time permits after core features are complete.
2. **Start Small:** Build in the order the requirements document lays out — holdings CRUD, then prices and valuation, then news, then sentiment, then impact, then the linked view. Each step should leave the system runnable and demonstrable. A finished medium-difficulty project presents far better than an 80%-complete hard one.
3. **External API Resilience:** Every project should demonstrate fallback behavior when an external API is unavailable. Because reads are served from the database and every response carries `asOf`/`stale`, a dead upstream degrades to older data instead of an error — the demo should show this explicitly by switching a provider to Mock mid-session.
4. **Quality Over Quantity:** A polished 3-ticker demo with clean UI and working sentiment beats a buggy 20-ticker system.
5. **Stay Agile:** The single biggest problem teams face is starting with a data model that is too complex. The nine tables here are the minimum the requirements need, not an aspiration — resist adding more. Two shapes are worth getting right on the first pass because changing them later means rewriting a migration and every query that touches it: the many-to-many link between news and symbols, and the unique constraint that makes deduplication work.
6. **Scope Excluded:** Multi-user, real order execution, tick-level intraday data, currency conversion (all USD, all US equities), full-article analysis (headlines only), and market-wide news scanning beyond the watchlist.

---

## License

This project is developed as part of the Final Project training program.

---

> **"We don't just show you the news. We show you what the news means for your money."**
