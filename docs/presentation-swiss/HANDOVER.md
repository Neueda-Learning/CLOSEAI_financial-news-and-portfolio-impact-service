# Presentation Deck Handover · FNPIS

> Status: Swiss Modernist version ready for a 15-minute team presentation, with in-browser editable text mode
> Last Updated: 2026-07-29

---

## Overview

This folder contains the **Swiss Modernist style presentation deck** for the Financial News & Portfolio Impact Service project.

### Files

- `index.html` — Complete presentation (13 slides, single-file, no external dependencies except Google Fonts + Lucide icons CDN)
- `HANDOVER.md` — This document
- `SPEAKER_RUNBOOK.md` — Team page ownership, timing, and rehearsal scripts
- `images/` — Image assets folder (currently empty; deck uses CSS-drawn graphics)

---

## How to Use

### Local Preview

```bash
# macOS - Open directly in browser
open index.html

# Or just double-click the file to open in your default browser
```

No server required. All styles, scripts, and graphics are embedded or loaded from CDN (Google Fonts, Lucide Icons, Motion One).

### Navigation

During presentation:

| Input | Action |
|-------|--------|
| `→` / `Space` / Click | Reveal next bullet point (step through `/pause` markers) |
| `←` / `↑` | Hide last revealed bullet or move backward |
| `↓` | Reveal forward or move to the next slide |
| `Home` / `End` | Jump to first/last slide |
| `ESC` | Toggle slide index overlay |
| `B` | Toggle low-power mode (disables WebGL background) |
| `E` / top-right `✎` | Toggle in-browser text editing |
| `D` / top-right `⇩` | Download the current edited HTML deck |
| Top-right `‹` / `›` | Move backward / forward |

### Editing Content

For quick rehearsal edits, open the deck in a browser and press `E` or the top-right `✎` button:

1. Click any highlighted text block
2. Edit the wording directly on the slide
3. Press `ESC` to exit editing mode and resume presenting
4. Press `D` or click the top-right `⇩` button to download a copy with your edits

For durable edits, open `index.html` in a text editor (VS Code, Sublime, etc.):

1. Find the slide section you want to edit (search for the title text)
2. Edit the text directly inside `<p>`, `<span>`, or other elements
3. Save the file
4. Refresh the browser to see changes

**Common edits:**
- Text content: search and replace directly
- Font size: look for `font-size:Xvw` or `max(Ypx, Zvw)` in `style` attributes
- Colors: use CSS variables `--accent` (IKB blue), `--ink` (black), `--paper` (white)
- Spacing: adjust `gap`, `padding`, `margin` values

### Online Publishing (Optional)

To publish on GitHub Pages:

1. Ensure the deck is in a `docs/` folder in your repo
2. Go to **Settings → Pages** in your GitHub repo
3. Set source to `Deploy from a branch`, select `master` (or your branch), folder `/docs`
4. Save — your presentation will be live at `https://<org>.github.io/<repo>/presentation-swiss/` in a few minutes

---

## Slide Breakdown

| # | Title | Layout | Notes |
|----|-------|--------|-------|
| 1 | Cover | S01 Hero | Product opener |
| 2 | Our Team Members | S04/Grid | 5 member cards with placeholder portraits |
| 3 | The Problem | S03 Split | Story-driven reason for the website |
| 4 | What The Website Does | S11 Timeline | Plain-language product loop |
| 5 | Breaking News Page | S08/Duo | Current finance-news demo candidate |
| 6 | Additional Features | S06/Center screenshot + callouts | Timothy uses arrows to explain 53% agreement, confirmed/divergent/inconclusive, sentiment, and coverage |
| 7 | Architecture | S05 Three Layers | Evan owns the system story |
| 8 | External Data & Fallback | S05 Three Layers | Finnhub news, Finnhub price, Mock/stale fallback |
| 9 | Sentiment & Impact Engine | S08/Duo | Model identity + three constraint layers, revealed one at a time |
| 10 | Honest Output | S06/KPI Tower | Direction vs alignment states, plus the no-advice boundary |
| 11 | How It Fails | S04/Grid | Six failure decisions: fail-fast boot, provider fallback, write-once verdict, edge cases, refresh lock, Testcontainers |
| 12 | 5-Day Build Plan | S02/Timing | Working slice every day |
| 13 | Closing | S10 Split Closing | 3 takeaways + thank you |

## Team Speaker Ownership

Detailed scripts live in `SPEAKER_RUNBOOK.md`.

| Speaker | Slides |
|---------|--------|
| Venessa Feng | 1–3 |
| David Hu | 4–5 |
| Timothy Xue | 6, 12–13 |
| Evan Li | 7–8 |
| Ethan Sun | 9–11 |

---

## Known Limitations

1. **WebGL background requires modern browser** — Chrome 60+, Firefox 55+, Safari 11+. Falls back gracefully on older browsers (no visual, but content still readable).

2. **Motion animation requires Motion One library** — Loads from CDN by default. If offline, install `motion` locally and update the import path in the `<script>` tag.

3. **No external images currently** — All diagrams/charts are CSS-drawn (grid layouts, color blocks, SVG icons). To add photos/screenshots, place them in `images/` and reference with `<img src="images/filename.png">`.

4. **Slide count fixed at 13** — Modifying the deck structure (adding/removing slides) requires updating:
   - The slide counter display
   - Navigation dot count
   - Timeline/progression logic (if you add custom timing)

---

## Customization Tips

### Change the Accent Color

All instances of IKB blue (`#002FA7`) are stored in the CSS `:root` variable `--accent`. To change to a different color:

1. Find the `<style>` section, search for `:root { `
2. Change `--accent: #002FA7;` to your color (e.g., `#FF6B35;`)
3. Save and refresh

### Adjust Overall Scale

All font sizes and spacing use viewport-relative units (`vw`, `vh`) to scale responsively. To make everything bigger/smaller:

1. Find `.h-xl { font-size: 3.2vw; }` (or similar)
2. Multiply all vw values by a factor (e.g., 1.2x to enlarge: `3.2vw` → `3.84vw`)
3. Repeat for other text styles and spacing variables

### Toggle WebGL Background

Line in CSS: `body.canvas-mode { background: var(--paper); }`

To disable the animated background by default:
- Remove `canvas-mode` class from `<body>` tag
- Users can still press `B` during presentation to toggle it

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Animation looks janky / choppy | Your device may be under load. Press `B` to disable WebGL background and use lower-power mode. |
| Text is cut off at bottom | Reduce font sizes or slide content volume. Check `max-height` constraints on containers. |
| Fonts look blurry | Ensure Fonts loaded from Google Fonts CDN (check Network tab). Try `font-smoothing: antialiased;` in CSS. |
| Keyboard navigation doesn't work | Click into the slide area first to give it focus, then try arrow keys. |
| Browser doesn't open file | Use `file:///` protocol in address bar, or serve locally with `python3 -m http.server 8000` and visit `localhost:8000`. |

---

## Credits

- **Design system**: Swiss International Style modernism (grid, color, typography)
- **Animation**: Motion One library (via CDN)
- **Icons**: Lucide icon set
- **Fonts**: Google Fonts (Inter, Noto Sans SC, JetBrains Mono)
- **Framework**: Vanilla HTML/CSS/JavaScript (no build tools required)

---

*Last Maintenance: 2026-07-29 · Questions? Check the repo's main README or ask the team.*
