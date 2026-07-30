# Presentation Speaker Runbook · FNPIS

> Based on the 14-slide Swiss HTML deck, `ReadMe.md`, `frontend/README.md`, and recent git comments.
> Goal: a complete 15-minute team presentation with Timothy owning the demo and Evan owning architecture.

---

## Source cues used

- `ReadMe.md`: product problem, honest output, provider fallback, feature priority order, implementation conventions
- `frontend/README.md`: breaking-news impact surface, chart tabs, holdings selector, mock-data honesty
- Recent git comments: restored editable Swiss HTML deck, frontend news surface, provider fallback pipeline, reviewer reliability concerns

---

## Page ownership

| Slides | Speaker | Section job |
|---|---|---|
| 1–3 | Venessa Feng | Open the product, introduce the team, make the problem feel real |
| 4–6 | David Hu | Explain the user journey, breaking-news example, and product loop |
| 7 | Timothy Xue | Run the live demo walkthrough |
| 8–9 | Evan Li | Explain architecture and data reliability |
| 10–12 | Ethan Sun | Explain sentiment, impact states, and engineering quality |
| 13–14 | Timothy Xue | Close with build plan and takeaways |

---

## 15-minute pacing

| Speaker | Target time | Slides |
|---|---:|---|
| Venessa Feng | 0:00–3:00 | 1–3 |
| David Hu | 3:00–6:00 | 4–6 |
| Timothy Xue | 6:00–7:45 | 7 |
| Evan Li | 7:45–10:30 | 8–9 |
| Ethan Sun | 10:30–13:15 | 10–12 |
| Timothy Xue | 13:15–15:00 | 13–14 |

Tip: the deck has 14 slides, but it still fills 15 minutes because the demo slide gets more time.

---

## Venessa Feng script · Slides 1–3

### Slide 1 — Cover

“Hi everyone, we’re Team CLOSEAI, and this is our Financial News & Portfolio Impact Service.
The product answers a practical question: when finance news breaks, what does it mean for the stocks in my portfolio?”

“Our goal is not just to show more news. The goal is to connect a headline, a holding, a market move, and an honest impact state in one view.”

### Slide 2 — Our Team Members

“Here are the five members of the team: Evan, David, Venessa, Ethan, and Timothy.
Each person will cover the part of the story closest to their work, so the presentation moves from why the product matters into how the system actually works.”

### Slide 3 — The Problem

“The problem is that headlines move faster than people can connect them to their own holdings.
An investor might see a story about Apple, Tesla, Microsoft, or Seagate, but they still have to manually ask: do I own this company, is the news positive or negative, did the price actually react, and how much does that matter to my position?”

“That gap is why this website exists. We are not replacing investment judgment; we are making the first connection faster and clearer.”

Transition:

“David will now show how that problem turns into a user journey and a concrete breaking-news example.”

---

## David Hu script · Slides 4–6

### Slide 4 — User Journey

“The user journey starts with the portfolio, not with a random news feed.
First the user has holdings. Then the system fetches company-linked news, scores the headline with the LLM, checks the price reaction, weights that by position size, and finally shows an honest verdict.”

“This matters because the output is portfolio-specific. A headline is only useful when the user can see whether it affects something they actually own.”

### Slide 5 — Breaking News Page

“This breaking-news slide shows the kind of event the product is designed for.
The Seagate example is useful because it has a clear headline, financial evidence, a ticker, and a visible market response.”

“The page should not stop at saying ‘positive news.’ It should show the evidence, the price response, and whether the market confirmed the signal.”

### Slide 6 — What the Website Does

“The product loop is simple: watch holdings, pull ticker-specific news, score sentiment, join price movement, and serve the linked impact view.
The important design choice is that the frontend gets a complete view from one backend endpoint, instead of stitching together news, prices, and impact logic itself.”

Transition:

“Timothy will now walk through the demo path we will show live.”

---

## Timothy Xue script · Slide 7

### Slide 7 — Complete Demo Flow

“This is the live demo path.
We open the portfolio, trigger or show breaking news, reveal sentiment, show the chart marker, read the impact, and then demonstrate the fallback path if we need to.”

“The demo should feel calm. If live provider data is slow or stale, we switch to the cached or Mock story and explain that freshness is visible by design.”

“The important line to say out loud is: the user does not just read the headline — they see the headline connected to the holding, the price move, and the final impact state.”

Transition:

“Evan will now explain the architecture that makes that demo path stable.”

---

## Evan Li script · Slides 8–9

### Slide 8 — Architecture

“The architecture is cache-first.
The UI reads from the database. Scheduled jobs fetch external data and land it before the frontend asks for it.”

“That gives us two benefits: reads stay fast, and the system can show stale/asOf labels when upstream data is limited instead of simply failing.”

### Slide 9 — External Data & Fallback

“We deliberately split external data responsibilities.
Finnhub company-news uses one key, Finnhub quotes and candles use another key, and sentiment uses the LLM API.”

“That separation matters because news polling should not starve quote refresh. If a provider is unavailable, cached data plus stale/asOf keeps the page honest, and Mock providers keep the rehearsal path stable.”

Transition:

“Ethan will explain how sentiment and impact are handled once the data is in the system.”

---

## Ethan Sun script · Slides 10–12

### Slide 10 — Sentiment & Impact Engine

“The LLM agent is useful only because we constrain it.
It must return a validated JSON shape: label, score, confidence, and rationale. Malformed output gets rejected, and the model version is stored so old results are traceable.”

“Impact is then stored as data, not recomputed casually on every page read.”

### Slide 11 — Honest Output

“This is one of the strongest ideas in the project: direction and alignment are separate.
Direction comes from sentiment: positive, negative, or neutral. Alignment comes from the price move: confirmed, divergent, or inconclusive.”

“Divergent is not a bug. If bad news appears but the stock rises, that disagreement is exactly the interesting signal the product should surface.”

### Slide 12 — Engineering Quality

“These details make the system credible.
Money and share counts use BigDecimal, monetary JSON values are strings, timestamps use UTC Instant, and enums are stored as strings.”

“Those sound like backend details, but they protect the demo from small errors that are easy to notice: rounding mistakes, wrong dates, unreadable historical states, or refresh races.”

Transition:

“We’ve shown the product, the architecture, and the honest output.
Timothy will close us out with the build plan and the final takeaway.”

---

## Timothy Xue script · Slides 13–14

### Slide 13 — 5-Day Build Plan

“As we close, this slide shows how we turn the idea into a working demo in five days.
We are not trying to build everything at once. We are building in the order that unblocks the team.”

“On day one, we set up the workflow as well as the foundation code.
That means Jira management for tracking tasks, CI integration so every push is checked, and the database schema, entities, and portfolio CRUD that unblock the rest of the team.”

“On days two and three, we focus on getting provider data into the system.
That means news polling, quote refresh, daily price bars, and the stale/asOf freshness state, so the frontend can read reliable cached data instead of calling APIs directly.”

“On day four, we connect the LLM sentiment agent to the impact recompute flow.
This is where the product becomes more than a dashboard: the system can say what the news implies, what the price actually did, and whether those two things agree.”

“On day five, we lock the demo.
We run CI, prepare the Mock fallback, rehearse the timing, and make sure the presentation still works even if a live provider is slow.”

“So the principle is simple: each day should leave us with a working slice, not a pile of unfinished pieces.”

### Slide 14 — Closing

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
