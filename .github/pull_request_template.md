<!--
  AI AGENT INSTRUCTIONS (not shown in rendered PR):
  Fill every section below. Delete no section — if a section does not apply, write "N/A" with a one-line reason.
  All PRs in this repo target `dev` (except hotfix and release PRs targeting `master`).
  Branch naming: feature/* | fix/* | docs/* | refactor/* | chore/* | hotfix/* | release/*
  Commits MUST use Conventional Commits: feat: | fix: | docs: | refactor: | test: | chore:
  Merge strategy: Merge Commit (not squash, not rebase).
  At least one human reviewer must approve. CI (lint + type-check + unit-test + build + commitlint) must pass.
-->

## Summary

<!-- One to three sentences: what changed and why. A reviewer should understand the purpose without reading the diff. -->

## Type

<!-- Exactly one. Match your branch prefix. -->

- [ ] `feat` — new capability (branch: `feature/*`)
- [ ] `fix` — bug correction (branch: `fix/*`)
- [ ] `docs` — documentation only, no code change (branch: `docs/*`)
- [ ] `refactor` — code restructure, no behavior change (branch: `refactor/*`)
- [ ] `test` — tests only, no production code change
- [ ] `chore` — tooling, CI, deps, config (branch: `chore/*`)

## Linked Task

Jira issue: <!-- paste full URL, e.g. FNPIS-42 -->

## Testing Performed

<!-- Every PR must describe how it was verified. Be specific — a reviewer will follow these steps. -->

- [ ] Unit tests pass locally
- [ ] Manual verification (describe below):

1.
2.
3.

## Visual Proof (frontend changes only)

<!-- If this PR touches UI code, paste before/after screenshots. Delete this section otherwise. -->

| Before | After |
|--------|-------|
| <!--  --> | <!--  --> |

## Self-Review Checklist

<!-- Every box must be checked before requesting review. -->

- [ ] Branch is up to date with `dev` (merged / rebased, conflicts resolved)
- [ ] All commits follow Conventional Commits format
- [ ] CI is green (lint, type-check, unit-test, build, commitlint)
- [ ] No leftover debug code (`console.log`, `System.out.println`, `print(`, `debugger`)
- [ ] No commented-out blocks (delete them — Git history preserves them)
- [ ] No secrets, keys, tokens, or passwords in the diff (`.env` is gitignored — verify no `.env` in changed files)
- [ ] No unrelated file changes (only files relevant to this PR's purpose)
- [ ] Breaking changes are documented in the PR description (if any)

## Reviewer Notes

<!-- Anything the reviewer should pay special attention to? Risky areas? Design decisions to discuss? Delete if empty. -->
