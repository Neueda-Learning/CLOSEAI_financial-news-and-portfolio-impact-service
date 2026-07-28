# CLAUDE.md — FNPIS (Financial News & Portfolio Impact Service)

## Project Identity

- **Project**: #15 — Financial News & Portfolio Impact Service
- **Team**: CLOSEAI (5 members)
- **Repo**: https://github.com/Neueda-Learning/financial-news-and-portfolio-impact-service
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

## Tech Stack (LOCKED)

| Layer | Choice | Notes |
|-------|--------|-------|
| Backend | **Java 17 + Spring Boot 3** | Non-negotiable |
| Frontend | React | Chart library TBD (Chart.js vs D3.js) |
| Database | **MySQL 8** | Dockerized for dev; JPA/Hibernate ORM |
| NLP | finBERT (ProsusAI) via Python Flask microservice | LLM API as fallback only |
| External APIs | Finnhub (primary) + Alpha Vantage (fallback) | Free tier, 60 req/min each |
| CI/CD | **GitHub Actions** | 5-job pipeline (see below) |
| Container | **Docker + Docker Compose** | 4 services: db, nlp-service, backend, frontend |
| Docs | Swagger / OpenAPI 3.0 | Auto-generated from Spring annotations |
| PM | **Jira** | Kanban, free tier |

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
| `nlp-test` | `nlp-service/` exists | pytest |
| `build` | Always | Docker compose verify + backend jar |

## Jira Setup

- **Project type**: Kanban (not Scrum)
- **Status flow**: To Do → In Progress → In Review → Done (Blocked at In Progress)
- **Issue types**: Epic (weekly), Story (P0-P3), Task (tech work), Bug
- **Labels**: `backend`, `frontend`, `nlp`, `devops`, `docs`
- **Views**: Kanban Board, By Assignee, By Epic, Backlog

## Project Files

| File | Purpose |
|------|---------|
| `ReadMe.md` | English project documentation (for humans) |
| `ReadMe.zh-CN.md` | Chinese project documentation (for humans) |
| `.github/pull_request_template.md` | PR template (self-contained for AI agents) |
| `.github/workflows/ci.yml` | CI pipeline definition |
| `commitlint.config.js` | Commit message rules |
| `.gitattributes` | Line ending normalization (LF for code) |
| `.gitignore` | Exclude secrets, deps, build artifacts |
| `.env.example` | Environment variable template |

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
| NLP approach | finBERT local (LLM API as fallback) | 2026-07-27 |

## Still Pending

| Item | Options | Blocker |
|------|---------|---------|
| Role assignments | 5 people → 3+ lead roles | Team discussion Week 1 |
| Frontend chart library | Chart.js vs D3.js | Frontend Lead decision |
| NLP final approach | finBERT only / both / LLM only | NLP Lead decision |
| E2E test framework | Cypress vs Playwright | Team discussion |
| API Keys | Finnhub + Alpha Vantage | Someone needs to register |
| Jira board | Needs to be created and populated | Team Lead |

## Domain Notes

- **P0-P3 priority** matches PDF spec exactly: Browse → View Metrics → Add → Remove
- **Presentation hook**: Side-by-side news vs price chart (the "wow moment")
- **External API resilience**: Cache all responses, demonstrate fallback during demo
- **Start small**: Portfolio(id, name) + Holding(id, portfolioId, ticker, shares) first
- **Quality > quantity**: 3 polished tickers > 20 buggy ones
- **Single user** assumed initially (auth optional, low priority)

## ⛔ HARD BAN: HSBC

**Under no circumstances mention "HSBC" anywhere** — not in code, comments, commits, PRs, docs, variable names, class names, API responses, test data, Jira issues, or any other project artifact. This is non-negotiable.

**If you see "HSBC" anywhere**: remove it immediately in a separate `fix/*` branch.

## Language & Documentation Rules

- **Code comments**: English
- **Commit messages**: English (Conventional Commits)
- **PR descriptions**: English
- **ReadMe**: English + Chinese (both are source of truth, independently maintained)
- **GitHub Issues / Jira**: English

## When Writing Code

- Match surrounding code style
- No `console.log` / `System.out.println` left in commits
- No commented-out code (Git history preserves it)
- No secrets in code (use `.env` — it's in `.gitignore`)
- Write tests for: impact correlator logic, sentiment score mapping, API fallback paths
- Run `./mvnw checkstyle:check` before push
- Update Swagger annotations when changing API endpoints
