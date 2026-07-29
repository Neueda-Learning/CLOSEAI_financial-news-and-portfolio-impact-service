# Presentation Speaker Runbook · FNPIS

> Based on the 15-slide Swiss HTML deck, `ReadMe.md`, `frontend/README.md`, and recent git comments.
> Goal: a complete 15-minute team presentation where each speaker owns consecutive slides.

---

## Source cues used

- `ReadMe.md`: product problem, honest output, provider fallback, feature priority order, implementation conventions
- `frontend/README.md`: breaking-news impact surface, chart tabs, holdings selector, mock-data honesty
- Recent git comments: restored editable Swiss HTML deck, frontend news surface, provider fallback pipeline, reviewer reliability concerns

---

## Consecutive page ownership

| Slides | Speaker | Section job |
|---|---|---|
| 1–3 | Venessa Feng | Open the product, introduce the team, make the problem feel real |
| 4–6 | David Hu | Turn the problem into user journey, requirements, and breaking-news proof |
| 7–9 | Evan Li | Walk the website loop, demo path, and architecture |
| 10–12 | Ethan Sun | Explain data reliability, sentiment engine, and honest output states |
| 13–15 | Timothy Xue | Close with engineering quality, build plan, and final takeaways |

---

## 15-minute pacing

| Speaker | Target time | Slides |
|---|---:|---|
| Venessa Feng | 0:00–3:00 | 1–3 |
| David Hu | 3:00–6:00 | 4–6 |
| Evan Li | 6:00–9:30 | 7–9 |
| Ethan Sun | 9:30–12:30 | 10–12 |
| Timothy Xue | 12:30–15:00 | 13–15 |

Tip: each slide should average about one minute. The demo slide can take slightly longer; the engineering/build slides should be tighter.

---

## Venessa Feng script · Slides 1–3

### Slide 1 — Cover

“Hi everyone, we’re Team CLOSEAI, and this is our Financial News & Portfolio Impact Service.
The product answers a very practical question: when finance news breaks, what does it mean for the stocks in my portfolio?”

“Our goal is not just to show more news. The goal is to connect a headline, a holding, a market move, and an honest impact state in one view.”

### Slide 2 — Our Team Members

“Here are the five members of the team: Evan, David, Venessa, Ethan, and Timothy.
For this presentation we each own a consecutive section, so the story moves smoothly from the problem to the product, then into the system and the build plan.”

### Slide 3 — The Problem

“The problem is that headlines move faster than people can connect them to their own holdings.
An investor might see a story about Apple, Tesla, Microsoft, or Seagate, but they still have to manually ask: do I own this company, is the news positive or negative, did the price actually react, and how much does that matter to my position?”

“That gap is why this website exists. We are not replacing investment judgment; we are making the first connection faster and clearer.”

Transition:

“David will now show how that problem becomes a user journey and a concrete demo example.”

---

## David Hu script · Slides 4–6

### Slide 4 — User Journey

“The user journey starts with the portfolio, not with a random news feed.
First the user has holdings. Then the system fetches company-linked news, scores the headline with the LLM, checks the price reaction, weights that by position size, and finally shows an honest verdict.”

“This matters because the output is portfolio-specific. A headline is only useful when the user can see whether it affects something they actually own.”

### Slide 5 — Requirements From README

“The README gives us a clean priority order.
P0 is browsing records: holdings, news, and impact assessments. P1 is metrics: portfolio value, per-holding valuation, and the side-by-side impact chart. P2 is adding holdings, and P3 is removing holdings.”

“So the MVP is narrow, but not shallow. It has enough flow to be useful: browse, understand, add, and remove.”

### Slide 6 — Breaking News Page

“This breaking-news slide shows the kind of event the product is designed for.
The Seagate example is useful because it has a clear headline, financial evidence, a ticker, and a visible market response.”

“The page should not stop at saying ‘positive news.’ It should show the evidence, the price response, and whether the market confirmed the signal.”

Transition:

“Evan will now walk through what the website actually does and how we demo it.”

---

## Evan Li script · Slides 7–9

### Slide 7 — What the Website Does

“The product loop is simple: watch holdings, pull ticker-specific news, score sentiment, join price movement, and serve the linked impact view.
The important design choice is that the frontend gets a complete view from one backend endpoint, instead of stitching together news, prices, and impact logic itself.”

### Slide 8 — Complete Demo Flow

“This is the live demo path.
We open the portfolio, trigger or show breaking news, reveal sentiment, show the chart marker, read the impact, and then demonstrate the fallback path if we need to.”

“The fallback step is not a weakness. It proves the demo is prepared for real provider problems.”

### Slide 9 — Architecture

“The architecture is cache-first.
The UI reads from the database. Scheduled jobs fetch external data and land it before the frontend asks for it.”

“That gives us two benefits: reads stay fast, and the system can show stale/asOf labels when upstream data is limited instead of simply failing.”

Transition:

“Ethan will explain the reliability layer and the impact logic behind those states.”

---

## Ethan Sun script · Slides 10–12

### Slide 10 — External Data & Fallback

“We deliberately split external data responsibilities.
Finnhub company-news uses one key, Finnhub quotes and candles use another key, and sentiment uses the LLM API.”

“That separation matters because news polling should not starve quote refresh. If a provider is unavailable, cached data plus stale/asOf keeps the page honest, and Mock providers keep the rehearsal path stable.”

### Slide 11 — Sentiment & Impact Engine

“The LLM agent is useful only because we constrain it.
It must return a validated JSON shape: label, score, confidence, and rationale. Malformed output gets rejected, and the model version is stored so old results are traceable.”

“Impact is then stored as data, not recomputed casually on every page read.”

### Slide 12 — Honest Output

“This is one of the strongest ideas in the project: direction and alignment are separate.
Direction comes from sentiment: positive, negative, or neutral. Alignment comes from the price move: confirmed, divergent, or inconclusive.”

“Divergent is not a bug. If bad news appears but the stock rises, that disagreement is exactly the interesting signal the product should surface.”

Transition:

“Timothy will close with the engineering quality rules, build plan, and final takeaway.”

---

## Timothy Xue script · Slides 13–15

### Slide 13 — Engineering Quality

“These details make the system credible.
Money and share counts use BigDecimal, monetary JSON values are strings, timestamps use UTC Instant, and enums are stored as strings.”

“Those sound like backend details, but they protect the demo from small errors that are easy to notice: rounding mistakes, wrong dates, unreadable historical states, or refresh races.”

### Slide 14 — 5-Day Build Plan

“The build plan is designed around dependencies.
Day one is schema and portfolio CRUD because entities and Flyway unblock everyone. Days two and three land provider data in the database. Day four connects the agent and impact recompute. Day five is CI, Mock fallback, and rehearsal.”

“The idea is to have a working slice every day, not five days of disconnected pieces.”

### Slide 15 — Closing

“The final takeaway is simple: read the news, see the impact.
The product connects one headline, one holding, one market move, and one honest state.”

“Thank you — we’re happy to take questions.”

---

## Demo walkthrough for rehearsal

1. Open the portfolio dashboard and name the portfolio state.
2. Select the affected holding so the audience knows the news is portfolio-linked.
3. Open the breaking-news / impact feed.
4. Reveal sentiment and confidence.
5. Show the price chart marker and daily movement.
6. Read direction, alignment, and value impact out loud.
7. Mention stale/asOf or Mock mode only if needed; do not apologize for it.

Rehearsal rule: if live data is slow, switch to the cached/Mock story immediately and say, “This is why the product exposes freshness instead of hiding provider state.”
