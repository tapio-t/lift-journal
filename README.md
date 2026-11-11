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
3. Ask Claude Code to start **Phase 0** of `docs/PLAN.md`. It reads `CLAUDE.md` automatically and will propose the project setup before creating anything.
4. Work phase by phase: approve each proposal, review the change, run the app, then merge.

## Entry format at a glance

```
deadlift 50 x 5              → 250 kg
squat 100 x 5, 5, 110 x 3    → weight change mid-exercise
squat (60 x 10), 100 x 5     → set in ( ) is not recorded
pull-up bw x 8, 8            → bodyweight × reps (bodyweight set in Goals & Settings)
push-up 0.5 bw x 20          → fraction of bodyweight
```

The full rules are in `docs/SPEC.md` §4.
