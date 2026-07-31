# Presentation Speaker Runbook · FNPIS

> Based on the 15-slide Swiss HTML deck, `ReadMe.md`, `frontend/README.md`, and recent git comments.
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
| 6–7 | Timothy Xue | Run the live demo walkthrough |
| 8–9 | Evan Li | Explain architecture and data reliability |
| 10–12 | Ethan Sun | Explain sentiment, impact states, and failure design |
| 13–15 | Timothy Xue | Close with build plan, takeaways, and team photo |

---

## 15-minute pacing

| Speaker | Target time | Slides |
|---|---:|---|
| Venessa Feng | 0:00–3:00 | 1–3 |
| David Hu | 3:00–5:30 | 4–5 |
| Timothy Xue | 5:30–7:30 | 6–7 |
| Evan Li | 7:30–10:15 | 8–9 |
| Ethan Sun | 10:15–13:00 | 10–12 |
| Timothy Xue | 13:00–15:00 | 13–15 |

Tip: the deck has 15 slides, but it still fills 15 minutes because the closing photo page is a short final beat.

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

“Start with the news: imagine reading that Apple cuts its iPhone production forecast.
The story is important, but the investor still needs the answer that matters: do I own this company, is the news positive or negative, did the price actually react, and how much does that matter to my position?”

“That is the gap: a headline can move a holding before a person has time to connect the dots.
We are not replacing investment judgment; we are making the first connection faster and clearer.”

Transition:

“David will now show how the website turns that problem into a simple product flow.”

---

## David Hu script · Slides 4–5

### Slide 4 — What the Website Does

“This slide explains the website in the simplest possible way.
The user starts with the stocks they actually own.
Then the website finds news connected to those stocks.”

“After that, the LLM analyses the headline and gives a simple signal:
positive, negative, or neutral.
But we also check the market, because a headline alone is not enough.
If the price moves in the same direction, that supports the signal.
If it moves the other way, that disagreement is also useful.”

“The final step is portfolio metrics.
Instead of showing news in one place and portfolio data somewhere else,
the website brings them together as sentiment, confidence, portfolio impact,
and alignment, so the user can understand what changed and why it matters.”

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

## Timothy Xue script · Slides 6–7

### Slide 6 — Additional Features

“For this slide, I will not read the text on screen.
I will use it as a map for the live demo.
The story is simple: I hold AAPL, news breaks, and the dashboard tells me whether that news really matters to my portfolio.”

“First I point to the 53 percent direction agreement.
That number checks whether the news direction matches the price direction.
In this example, 23 confirmed records out of 43 clear records gives roughly 53 percent.”

“Then I use the three state boxes to explain the result.
Confirmed means the news and market price move in the same direction.
Divergent means the news says one thing, but price action says the opposite.
Inconclusive means the move is too small, neutral, or unclear to judge.”

“Next I point to weighted sentiment.
This is not a simple count of positive and negative news.
It is weighted by the portfolio holding size, so a bigger position has more influence than a tiny position.”

“Then I point to news coverage.
Coverage answers: how many of my portfolio symbols had at least one analysed news item today?
If it is 100 percent, every holding had news coverage; if it is lower, the user knows the picture is incomplete.”

“The stronger story is that the system makes the result explainable:
agreement, market confirmation, portfolio sentiment, and coverage.”

Transition:

“Now let’s leave the slide and walk through the frontend.”

---

### Slide 7 — Walk Through The Frontend

“This is where we switch from explaining the feature to using it.
I will start from the news feed, connect the headline to the portfolio impact, and then show the agreement signals.”

“The goal is simple: the audience should see the same path a user would take in the frontend.”

Transition:

“Evan will now explain the architecture that makes that demo path stable.”

---

## Evan Li script · Slides 8–9

### Slide 8 — Three Layers System Architecture

“This diagram shows the system layers from left to right.
News and price data come in from providers, the backend normalizes and stores them, and the frontend reads the prepared result instead of calling providers directly.”

“The important point is that the user sees one clean portfolio-impact view, but underneath it we separate ingestion, sentiment analysis, impact calculation, storage, and presentation.”

### Slide 9 — Land It Fast, Read It Fast

“This is the data flow behind the demo.
External providers land news, quotes, and sentiment into the database first.”

“Then the API reads prepared rows quickly, and the impact engine combines news plus price from the database before serving the portfolio impact endpoint.”

Transition:

“Ethan will explain how sentiment and impact are handled once the data is in the system.”

---

## Ethan Sun script · Slides 10–12

### Slide 10 — Sentiment & Impact Engine

> Four reveals on the right. Say the line, then click.

“We use an LLM here, but we give it a very small job.
It reads one headline and gives us three things back: a label, a score, and a confidence.
Nothing else.”

*(click)* “The model is Claude Opus, and we run it at temperature zero — so the same headline gives us the same answer every time.”

*(click)* “The job itself is small. One headline in, one JSON object out, with a token limit. There is not much room to go wrong.”

*(click)* “Then we check its work. If the score says negative but the label says positive, we keep the number and fix the label.”

*(click)* “And the last word is ours, not the model's. It proposes an answer; our code decides whether to save it.”

“We save that answer once, and we never rewrite it. So what you see on the page does not change behind your back.”

### Slide 11 — Honest Output

> Three cards. Say all three.

“Things go wrong. So we decided in advance how they should go wrong.”

“If a key is missing, or the database does not match the code, the app refuses to start. We would rather find out now than in the middle of this demo.”

### Slide 12 — How It Fails

“And our tests run against a real MySQL. A fake one would be faster, but it rounds money differently — and we would rather find that out here than in front of you.”

Transition:

“That's how the system behaves when things break.
Timothy will take it from here.”

---

## Timothy Xue script · Slides 13–15

### Slide 13 — 5-Day Sprint

“As we close, this slide shows the five-day sprint.
Each day left us with a working slice, not a pile of unfinished pieces.”

“Day one was Schema + CRUD + CI.”

- Flyway V1–V9
- JPA entities
- Portfolio CRUD
- CI foundation
- Docker Compose

“Day two was Provider pipelines.”

- News fetch + dedup
- 3-provider quote chain
- Daily close snapshot
- Stale/asOf handling

“Day three was Agent + impact + frontend.”

- LLM sentiment engine
- Impact mapping + verification
- React dashboard

“Day four was Polish + v0.1.1 release.”

- 250+ commits
- 50 PRs
- 150 JIRA tasks
- All 9 JIRA stories told
- 10+ project documents

“Day five is Today presentation.”

- Live demo
- Story walkthrough
- Final Q&A
- Delivery

### Slide 14 — Closing

“The final takeaway is simple: read the news, see the impact.
The product connects one headline, one holding, one market move, and one honest state.”

“Thank you — we’re happy to take questions.”

### Slide 15 — Team Photo

“This is Team CLOSEAI.
Five people, one product, one clear finish.”

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
