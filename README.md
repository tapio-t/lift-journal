# Lift Journal

A minimal black-and-white fitness journal for Android.

You type your training as plain text, for example `deadlift 50 x 5, 5, 5`. The app turns it into progress charts and goal tracking. Everything stays on your phone, and it's backed up in real time to a folder you choose.

**Status:** a working HTML prototype is done. The native Android app (Kotlin + Jetpack Compose) is next, developed with Claude Code in Android Studio.

## Repository layout

| Path | What it is |
|---|---|
| `CLAUDE.md` | Working rules for Claude Code. It always proposes before implementing. |
| `docs/SPEC.md` | Product specification: screens, entry format, calculations, data model |
| `docs/PLAN.md` | Tech stack, architecture, phased plan with acceptance checks, open questions |
| `prototype/` | The HTML prototype (open `lift-journal.html` in Chrome) and notes on what not to port |
| `testdata/` | Golden parser cases, a prototype backup sample, and a text-import sample |

The Android project (`app/`, Gradle files) is created in phase 0 of `docs/PLAN.md`.

## Getting started in Android Studio with Claude Code

1. Clone the repository and open the folder in Android Studio.
2. Open Android Studio's terminal at the repository root and start Claude Code with `claude`. See the [Claude Code docs](https://docs.claude.com/en/docs/claude-code/overview) for installation.
3. Ask Claude Code to build **pull request #1, the whole working app** (see "Delivery" in `docs/PLAN.md`). It reads `CLAUDE.md` automatically and proposes the full implementation once before creating anything.
4. After you approve, it builds all phases on `feature/full-app`, with a commit and a short status checkpoint per phase. Try the debug build at any checkpoint, then merge when every acceptance check passes.
5. Later fixes and features go in small, separate pull requests.

Suggested first message:

> Read CLAUDE.md and docs/PLAN.md. Propose pull request #1: the whole working app, all phases 0–8, on branch feature/full-app. Include structure, dependencies, database schema, screens, order of work and risks. Don't create anything until I approve.

## Entry format at a glance

```
deadlift 50 x 5              → 250 kg
squat 100 x 5, 5, 110 x 3    → weight change mid-exercise
squat (60 x 10), 100 x 5     → set in ( ) is not recorded
pull-up bw x 8, 8            → bodyweight × reps (bodyweight set in Goals & Settings)
push-up 0.5 bw x 20          → fraction of bodyweight
```

The full rules are in `docs/SPEC.md` §4.
