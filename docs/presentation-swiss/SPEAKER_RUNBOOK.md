# Presentation Speaker Runbook · FNPIS

> Based on the 13-slide Swiss HTML deck, `ReadMe.md`, `frontend/README.md`, and recent git comments.
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
| 4–5 | David Hu | Explain the product loop and breaking-news example |
| 6 | Timothy Xue | Run the live demo walkthrough |
| 7–8 | Evan Li | Explain architecture and data reliability |
| 9–11 | Ethan Sun | Explain sentiment, impact states, and failure design |
| 12–13 | Timothy Xue | Close with build plan and takeaways |

---

## 15-minute pacing

| Speaker | Target time | Slides |
|---|---:|---|
| Venessa Feng | 0:00–3:00 | 1–3 |
| David Hu | 3:00–5:30 | 4–5 |
| Timothy Xue | 5:30–7:30 | 6 |
| Evan Li | 7:30–10:15 | 7–8 |
| Ethan Sun | 10:15–13:00 | 9–11 |
| Timothy Xue | 13:00–15:00 | 12–13 |

Tip: the deck has 13 slides, but it still fills 15 minutes because the demo slide gets more time.

---

## Venessa Feng script · Slides 1–3

### Slide 1 — Cover

“Hi everyone, we’re Team CLOSEAI, and this is our Financial News & Portfolio Impact Service.
The product answers a practical question: when finance news breaks, what does it mean for the stocks in my portfolio?”

“Our goal is not just to show more news. The goal is to connect a headline, a holding, a market move, and an honest impact state in one view.”

### Slide 2 — Our Team Members

“Here are the five members of the team: Venessa, Ethan, Timothy, David, and Evan.
Each person will cover the part of the story closest to their work, so the presentation moves from why the product matters into how the system actually works.”

### Slide 3 — The Problem

“The problem is that headlines move faster than people can connect them to their own holdings.
An investor might see a story about Apple, Tesla, Microsoft, or Nvidia, but they still have to manually ask: do I own this company, is the news positive or negative, did the price actually react, and how much does that matter to my position?”

“That gap is why this website exists. We are not replacing investment judgment; we are making the first connection faster and clearer.”

Transition:

“David will now show how the website turns that problem into a simple product flow.”

---

## David Hu script · Slides 4–5

### Slide 4 — What the Website Does

“This slide explains the website in the simplest possible way.
The user starts with the stocks they actually own.
Then the website finds news connected to those stocks.”

“After that, the AI reads the headline and gives a simple signal:
positive, negative, or neutral.
But we also check the market, because a headline alone is not enough.
If the price moves in the same direction, that supports the signal.
If it moves the other way, that disagreement is also useful.”

“The final step is the impact.
Instead of showing news in one place and portfolio data somewhere else,
the website brings them together so the user can understand what changed
and why it matters to their own holdings.”

Transition:

“Now that the product flow is clear, the next slide shows the kind of breaking-news event
this product is built for.”

### Slide 5 — Breaking News Page

“This breaking-news slide shows the kind of event the product is designed for.
Here we use Apple as the example: ‘Apple tops $5 trillion market cap.’
The news time shown on the slide is Tuesday, July 28, 2026 at 5:10 PM.”

“The page should not stop at saying ‘positive news.’
It shows the ticker, the sentiment, the confidence, the portfolio impact,
and whether the market confirmed the signal.”

“For this example, the signal is clean.
The article is POSITIVE with a 0.90 score and 95 percent confidence.
The AAPL holding shows a positive impact of $391.39.
The alignment is CONFIRMED, which means the news says the stock should rise,
and the price did rise.”

Transition:

“Timothy will now walk through the demo path we will show live.”

---

## Timothy Xue script · Slide 6

### Slide 6 — Complete Demo Flow

“This is the live demo path.
We open the portfolio, trigger or show the Apple breaking-news example,
reveal sentiment, show the chart marker, read the impact,
and then demonstrate the fallback path if we need to.”

“The demo should feel calm. If live provider data is slow or stale, we switch to the cached or Mock story and explain that freshness is visible by design.”

“The important line to say out loud is: the user does not just read the headline — they see the headline connected to the holding, the price move, and the final impact state.”

Transition:

“Evan will now explain the architecture that makes that demo path stable.”

---

## Evan Li script · Slides 7–8

### Slide 7 — Architecture

“The architecture is cache-first.
The UI reads from the database. Scheduled jobs fetch external data and land it before the frontend asks for it.”

“That gives us two benefits: reads stay fast, and the system can show stale/asOf labels when upstream data is limited instead of simply failing.”

### Slide 8 — External Data & Fallback

“We deliberately split external data responsibilities.
Finnhub company-news uses one key, Finnhub quotes and candles use another key, and sentiment uses the LLM API.”

“That separation matters because news polling should not starve quote refresh. If a provider is unavailable, cached data plus stale/asOf keeps the page honest, and Mock providers keep the rehearsal path stable.”

Transition:

“Ethan will explain how sentiment and impact are handled once the data is in the system.”

---

## Ethan Sun script · Slides 9–11

### Slide 9 — Sentiment & Impact Engine

> Four reveals on the right. Say the line, then click.

“We use an LLM here, but we give it a very small job.
It reads one headline and gives us three things back: a label, a score, and a confidence.
Nothing else.”

*(click)* “The model is Claude Opus, and we run it at temperature zero — so the same headline gives us the same answer every time.”

*(click)* “The job itself is small. One headline in, one JSON object out, with a token limit. There is not much room to go wrong.”

*(click)* “Then we check its work. If the score says negative but the label says positive, we keep the number and fix the label.”

*(click)* “And the last word is ours, not the model's. It proposes an answer; our code decides whether to save it.”

“We save that answer once, and we never rewrite it. So what you see on the page does not change behind your back.”

### Slide 10 — Honest Output

“We answer two questions, and we keep them apart.
First: what does the news say? Positive, negative, or neutral.
Second: did the price agree? That is where confirmed, divergent, and inconclusive come from.”

“Both of these just describe what happened. Neither one says buy or sell. And a system that can say ‘we don't know’ is not giving advice.”

### Slide 11 — How It Fails

> Six cards. Do not read all six — open with 01, pick two from the middle, close on 06.

“Things go wrong. So we decided in advance how they should go wrong.”

“If a key is missing, or the database does not match the code, the app refuses to start. We would rather find out now than in the middle of this demo.”

“If a price source goes down, we switch to another one, and the rest of the system never notices.”

“Every article gets one answer, written once. If the call fails, we say so — we do not quietly save a neutral and pretend we asked.”

“We wrote down twenty-four edge cases. Seventeen have a test. Three of them are divide-by-zero traps, and we hit those on purpose.”

“And the tests run against a real MySQL, not an in-memory stand-in — because decimal precision only behaves like production on the real thing.”

Transition:

“We’ve shown the product, the architecture, and the honest output.
Timothy will close us out with the build plan and the final takeaway.”

---

## Timothy Xue script · Slides 12–13

### Slide 12 — 5-Day Build Plan

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

### Slide 13 — Closing

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
