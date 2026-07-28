# CLAUDE.md — FNPIS (Financial News & Portfolio Impact Service)

## Project Identity

- **Project**: #15 · Group 7 — Financial News & Portfolio Impact Service
- **Team**: CLOSEAI (5 members)
- **Repo**: https://github.com/Neueda-Learning/CLOSEAI_financial-news-and-portfolio-impact-service
- **Timeline**: 6 weeks
- **Presentation**: 15 min + 5 min Q&A

## Team

| Name | Role |
|------|------|
| Evan Li | TBD |
| David Hu | TBD |
| Venessa Feng | TBD |
| Ethan SUN | TBD |
| Timothy Xue | TBD |

> Role assignments (Backend Lead / NLP Lead / Frontend Lead / additional roles) — decide Week 1.

Instructors (GitHub viewers): `helppo2`, `tuistmessiah`

## Source of Truth

**The architecture document is authoritative for all technical decisions.** When this
file disagrees with it, the architecture document wins and this file is the bug.

| Document | Covers |
|----------|--------|
| [`docs/项目15-①需求文档.md`](docs/项目15-①需求文档.md) | Requirements A–G, sentiment rules (§5.2), impact formulas with worked examples (§5.3), edge cases EC-01–EC-24 (§8) |
| [`docs/项目15-②架构设计.md`](docs/项目15-②架构设计.md) | **Layering, decisions 1–6, data model, project structure. Read this before writing code.** |
| [`docs/项目15-③API契约.md`](docs/项目15-③API契约.md) | Endpoints, pagination, error format. Superseded by Swagger once the backend is implemented (G3) |

## Tech Stack (LOCKED)

Per architecture §0 and §6.1.

| Layer | Choice | Notes |
|-------|--------|-------|
| Backend | **Java 17 + Spring Boot 3** | Non-negotiable |
| Frontend | SPA, framework chosen by the frontend dev | **Chart.js 4** + annotation plugin is required (the news marker line in F4) |
| Database | **MySQL 8** + Flyway | utf8mb4 throughout; `ddl-auto: validate` — Flyway owns the schema |
| Sentiment | **LLM Agent, single engine** | No finBERT, no Python service, no multi-engine comparison (decision 5) |
| External APIs | **Finnhub only** — separate key for news and for prices | Two endpoints, two accounts, one rate limiter per key (§6.5) |
| HTTP client | RestClient (Spring 6.1+) | — |
| Resilience | Resilience4j | Rate limit, retry, circuit breaker (decision 1) |
| Local cache | Caffeine + Spring Cache | In front of outbound provider calls only, never read endpoints (decision 2) |
| CI/CD | **GitHub Actions** | See pipeline below |
| Container | **Docker + Docker Compose** | 3 services: mysql, backend, frontend |
| Docs | springdoc-openapi | Generated from annotations, never hand-written YAML |
| PM | **[Jira](https://therain2026.atlassian.net/jira/software/projects/FNPIS/boards/3)** | Kanban, free tier |

## Architecture Essentials

Full reasoning in architecture §2 and §3. The five things that matter most day to day:

**Layering.** `api → service → repository → domain`, with `integration/` (anti-corruption
layer) and `scheduler/` as side paths. The system has two entry points: HTTP and the clock.

| Layer | May do | Must never do |
|-------|--------|---------------|
| api | Validation, DTO conversion, auth | Business logic |
| service | All business rules, transactions | Call third-party HTTP directly |
| integration | Call third parties, cache, retry, map formats | Business rules |
| scheduler | Trigger tasks | Implement logic (delegate to services) |
| repository | Read and write | Compute |

**Three Provider interfaces** (`NewsProvider`, `PriceProvider`, `SentimentEngine`) —
services depend on the interface, never on an implementation. Finnhub DTOs must not
appear in any `service/` method signature; that leak is what makes swapping a data
source expensive. Watch the import direction of `integration/finnhub/` in review.

**Cache-first reads (decision 2).** Read endpoints query the database only. Every
external fetch is landed by a scheduled job first. Responses carry `asOf` and `stale`
so a dead upstream degrades to old data instead of an error.

**Impact results are persisted, not computed per request (decision 3).** The read path
serves `impact_assessment` rows.

**Money is `BigDecimal`, serialized as a JSON string.** `double` loses precision and
JavaScript's `Number` is a double, so the string boundary is deliberate. Checkstyle
enforces this. Times are `Instant`, stored UTC, converted at the display layer.

## Git Workflow (LOCKED)

```
master (release, protected)
  ├── hotfix/*     source: master → target: master
  └── dev (integration, protected)
        ├── feature/*   → dev
        ├── fix/*       → dev
        ├── docs/*      → dev
        ├── refactor/*  → dev
        ├── chore/*     → dev
        └── release/*   → master
```

- **NO direct push** to master or dev
- **Merge Commit** only (no squash, no rebase)
- **≥1 review** required for all PRs
- **Branch naming**: kebab-case with prefix (feature/, fix/, hotfix/, release/, docs/, refactor/, chore/)
- **Commit messages**: Conventional Commits — `feat | fix | docs | refactor | test | chore`
- **Hotfix flow**: branch master → PR master → tag (patch bump) → merge back to dev
- **Release flow**: dev → release/vX.Y.Z → PR master → tag → merge back to dev → delete release branch
- **Stale branches**: 1 week for draft PRs, 2 weeks for inactive branches, weekly Friday cleanup

## CI Pipeline (`.github/workflows/ci.yml`)

Triggers on PR and push to `dev`/`master`. Conditional activation — jobs skip if code directory doesn't exist yet:

| Job | When Active | What |
|-----|------------|------|
| `commitlint` | Always | Enforce Conventional Commits |
| `backend-lint` | `backend/` exists | Checkstyle |
| `backend-type-check` | `backend/` exists | javac compile |
| `backend-test` | `backend/` exists | JUnit 5 |
| `frontend-lint` | `frontend/` exists | ESLint |
| `frontend-type-check` | `frontend/tsconfig.json` exists | tsc --noEmit |
| `frontend-test` | `frontend/` exists | Jest + React Testing Library |
| `build` | Always | Docker compose verify + backend jar |

`build` seeds `.env` from `.env.example` before validating compose — the compose file
uses required-variable syntax, so interpolation fails outright without it. That also
makes the job a sync check on the template: add a required variable without listing it
in `.env.example` and this goes red.

**CI needs no real API keys.** Finnhub is stubbed with WireMock, sentiment with
`StubSentimentEngine` (architecture §7.5).

## Jira Setup

- **Project type**: Kanban (not Scrum)
- **Status flow**: TODO → IN PROCESS → IN REVIEW → COMPLETED (Blocked is its own column)
- **Issue types**: 长篇故事 (Epic), 故事 (Story), 子任务 (Subtask), Feature, 缺陷 (Bug)
- **Labels**: `backend`, `frontend`, `core-logic`, `demo-hook`, `test`, `data`, `api`, `infra`, `p0`, `p1`, `p2`
- **Views**: Kanban Board, By Assignee, By Epic, Backlog

## Project Files

| File | Purpose |
|------|---------|
| `docs/` | Requirements, architecture, API contract — see Source of Truth above |
| `ReadMe.md` | English project documentation (for humans) |
| `ReadMe.zh-CN.md` | Chinese project documentation (for humans) |
| `.github/pull_request_template.md` | PR template (self-contained for AI agents) |
| `.github/workflows/ci.yml` | CI pipeline definition |
| `commitlint.config.js` | Commit message rules — `type-enum` allows 6 types only |
| `.gitattributes` | Line ending normalization (LF for code; `mvnw` spelled out explicitly) |
| `.gitignore` | Exclude secrets, deps, build artifacts |
| `.env.example` | Environment variable template |
| `docker-compose.yml` | mysql + backend (frontend commented out until it has a Dockerfile) |
| `backend/checkstyle.xml` | Deliberately light — bans `System.out.print*` and `double`/`float` for money |

## Backend Package Layout

Per architecture §7.1. Packages marked *pending* are absent on purpose — every one of
them references `@Entity` in its method signatures, and the entities are not written
yet, so creating them now would leave the repo non-compiling for everyone.

| Package | Status | Contents |
|---------|--------|----------|
| `config/` | done | Jackson, Caffeine, OpenAPI |
| `domain/` | enums done, **entities pending** | `@Entity` + enums |
| `common/` | done | Paging envelope, `Freshness`, RFC 7807 types |
| `api/` | pending | `@RestController` + DTO; `internal/` for `/api/v1/**`, `pub/` for `/public/v1/**` |
| `service/` | pending | Business logic |
| `integration/` | pending | Provider interfaces + `finnhub/`, `mock/`, `sentiment/` |
| `scheduler/` | pending | `@Scheduled` tasks |
| `repository/` | pending | Spring Data JPA interfaces |

> `common/` is **not** in architecture §7.1 — it is an addition. The paging envelope,
> `Freshness`, and the error types are imported by all three developers, and putting
> them under `api/` would make one person's package own types everyone depends on.
> `common/` is accepted as a permanent addition. Sync it back to architecture §7.1.

## Ownership

| Area | Owner | Rule |
|------|-------|------|
| Flyway scripts + `@Entity` classes | requirement A's owner | **Nobody else touches these.** One person owns the schema |
| `repository/` interfaces | each developer | Write your own against the shared entities |
| Requirements B, C | second developer | — |
| Requirements D, E (sentiment + impact) | third developer | Blocked until entities land |

**Flyway rules.** V1–V6 are reserved (see `backend/src/main/resources/db/migration/`).
Never edit a committed script — Flyway stores a checksum, so a changed file makes every
other checkout fail on startup. Claim V7+ for later changes and tell the team.

## Known Decisions

| Item | Decision | Date |
|------|----------|------|
| Backend language | Java 17 (not TypeScript, not Java 21) | 2026-07-27 |
| Database | MySQL 8 (not PostgreSQL) | 2026-07-27 |
| Release branch | `master` (not `main`) | 2026-07-27 |
| Dev branch | `dev` (not `develop`) | 2026-07-27 |
| Merge strategy | Merge Commit (not squash/rebase) | 2026-07-27 |
| PM tool | Jira (not Trello) | 2026-07-27 |
| Bilingual docs | EN + zh-CN, independent writing | 2026-07-27 |
| Sentiment engine | **LLM Agent, single engine** — finBERT and the Python service dropped | 2026-07-28 |
| Data source | **Finnhub only**, separate key per purpose — Alpha Vantage dropped as planned fallback | 2026-07-28 |
| Chart library | **Chart.js 4** + annotation plugin | 2026-07-28 |
| Frontend framework | Frontend dev's choice — does not affect the API contract | 2026-07-28 |
| Package layout | Layer-based per architecture §7.1 | 2026-07-28 |
| Schema ownership | One owner for Flyway + entities; repositories written per-developer | 2026-07-28 |
| Integration test DB | Testcontainers with real MySQL, **not H2** | 2026-07-28 |
| Performance targets | **No SLO** — latency is dominated by the Finnhub poll interval | 2026-07-28 |

## Still Pending

| Item | Options | Blocker |
|------|---------|---------|
| Role assignments | 5 people → 3+ lead roles | Team discussion Week 1 |
| E2E test framework | Cypress vs Playwright | Team discussion |
| API keys | 2 Finnhub accounts + 1 LLM key | Someone needs to register |
| Jira board | Needs to be created and populated | Team Lead |
| Watchlist contents | Which 10–20 US large caps | Pick names with heavy news flow |
| `common/` package | Write back into architecture §7.1, or move under `api/` | Team decision |
| `impact_assessment` columns | Requirements §10.1 lists 6 fields; the contract's `impact-view` needs 11 plus `attribution_date` | Schema owner — build to the contract |
| `portfolio_valuation_snapshot` | Architecture §5 requires the job, §4.2 never defines the table | **Urgent** — every night that passes is a data point lost forever |

### Blocking research (day one, architecture §6.3 and §6.5)

| Priority | Question | Consequence if the answer is bad |
|----------|----------|----------------------------------|
| Highest | Does the free tier expose historical daily bars? | Requirement E loses its data source. Fallback is to accumulate bars from daily snapshots — **needs several days of lead time** |
| Highest | Is rate limiting per key or per IP? | If per IP, separate accounts on one machine buy nothing and the multi-key design collapses |
| High | Does the free tier permit one person holding several accounts? | Worst case is every account banned the day before the demo. Keep a Mock Provider config ready either way |

## Domain Notes

- **P0-P3 priority** matches PDF spec exactly: Browse → View Metrics → Add → Remove
- **Presentation hook**: Side-by-side news vs price chart (the "wow moment"), served by
  the single `GET /api/v1/news/{id}/impact-view` endpoint
- **The original logic is the sentiment + impact junction.** Everything else is plumbing;
  this is the part worth protecting in review
- **Two questions, kept separate**: what the news *should* have done to the position
  (`Direction`, from sentiment) and whether the price *actually* agreed (`Alignment`).
  Never blend them into one score — disagreement is the informative case
- **`INCONCLUSIVE` is a real answer**, covering both a move smaller than epsilon (noise)
  and a missing previous close (EC-18). Not a failure to report
- **External API resilience**: reads are served from the database, so a dead upstream
  yields old data with `stale: true` rather than an error. Demo this explicitly
- **Quality > quantity**: 3 polished tickers > 20 buggy ones
- **Single user** assumed initially (auth optional, low priority)
- **Scope excluded**: multi-user, real trading, tick-level data, currency conversion,
  full-article analysis (headlines only), market-wide news scanning

## Scheduled Jobs

Architecture §5. Dependency order matters — impact recompute needs both sentiment and
the closing snapshot to have finished, or it runs on incomplete data.

| Job | Cadence | Notes |
|-----|---------|-------|
| News poll | 15 min | Dedupe on `external_id` |
| Sentiment | After news lands | Only articles with no `sentiment_score` row |
| Quote refresh | 1 min, market hours | — |
| Closing snapshot | Daily after close | Writes `price_bar` + portfolio valuation. **Do not skip this** — F5's chart has no other source |
| Impact recompute | After the snapshot | Writes `impact_assessment` |

Use `fixedDelay`, not `fixedRate` — that alone prevents a job overlapping itself
(EC-23). Manual triggers arrive on an HTTP thread, so they still need an explicit lock;
failing to acquire it returns 409 rather than queueing (EC-20). Skipping is correct
here: the news poll is idempotent, and queueing just piles up requests that trip the
rate limiter when the upstream recovers.

## Language & Documentation Rules

- **Code comments**: English
- **Commit messages**: English (Conventional Commits)
- **PR descriptions**: English
- **ReadMe**: English + Chinese (both are source of truth, independently maintained)
- **GitHub Issues / Jira**: English

## When Writing Code

- Match surrounding code style
- No `console.log` / `System.out.println` left in commits (Checkstyle enforces the latter)
- No commented-out code (Git history preserves it)
- No secrets in code (use `.env` — it's in `.gitignore`)
- **`BigDecimal` for money and share counts, never `double`/`float`**
- **`Instant` stored UTC** for timestamps, `LocalDate` for trade dates
- **`@Enumerated(EnumType.STRING)`** on every enum column — ordinals break historical
  rows the moment someone inserts a value mid-enum
- Redact `token` query params and `Authorization` headers before logging
- Run `./mvnw checkstyle:check` before push
- Update Swagger annotations when changing API endpoints

## Testing

Architecture §7.2.

| Level | Tools | Focus |
|-------|-------|-------|
| Unit | JUnit 5 + Mockito | **The impact engine must have unit tests** — use the three worked examples from requirements §5.3 |
| Integration | `@SpringBootTest` + **Testcontainers MySQL** | Scheduled jobs, dedupe, Flyway scripts |
| Integration layer | **WireMock** | Field mapping, rate-limit retry, error handling |
| Sentiment validation | JUnit 5 | Feed malformed LLM responses: illegal label, score out of range, non-JSON, score disagreeing with label |
| E2E | Playwright or Cypress | All-Mock providers so results are deterministic |

**Testcontainers, not H2.** H2's compatibility mode differs from MySQL on `DECIMAL`
precision, date functions, and unique index length limits — a Flyway script that passes
on H2 can still fail on real MySQL, which makes the test worthless.

Requirements EC-01–EC-24 are written from real bug scenarios; aim for one test each.
Three are division-by-zero traps that must be covered: EC-07 (zero cost basis), EC-18
(missing previous close), EC-22 (zero total value when computing weight).
