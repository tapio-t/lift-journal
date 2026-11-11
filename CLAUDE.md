# CLAUDE.md — Lift Journal

Instructions for Claude Code working in this repository.

## Rule 1: always propose before implementing

**Never change code, configuration, dependencies or files before the owner has approved a proposal.** This applies to new features, bug fixes, refactors, dependency updates and "small" one-line changes alike.

For every task:

1. **Investigate.** Read the relevant code and docs, and reproduce the bug if there is one. Reading files, searching and running existing tests or builds is fine without approval.
2. **Propose** in chat:
   - What you will change, and why. For a bug: the root cause, with evidence.
   - Which files you will touch or create.
   - The approach, plus alternatives you considered if any are realistic.
   - Risks: data migration, loss of user data, behaviour changes.
   - How you will test it.
3. **Wait** for explicit approval ("yes", "go ahead", "approved"). If the owner changes the proposal, re-propose the changed parts.
4. **Implement exactly what was approved.** If you discover the change needs to be bigger or different, stop and propose again before continuing.
5. **Report:** what changed, the test results, and anything left open.

Ask instead of guessing when requirements are ambiguous. `docs/SPEC.md` and the prototype are the reference. If they disagree, ask.

## Project

Lift Journal is a minimal black-and-white fitness journal for Android. The user types sets as plain text, for example `deadlift 50 x 5, 5, 5`. The app calculates volume, top sets and goal progress.

- `docs/SPEC.md`: what the app does (authoritative product spec).
- `docs/PLAN.md`: tech stack, architecture and phased plan with acceptance checks. Work phase by phase.
- `prototype/lift-journal.html`: working HTML prototype. Open it in Chrome to see intended behaviour. Its JavaScript is the reference implementation for parsing and calculations.
- `testdata/parser-golden.json`: golden parser cases generated from the prototype. The Kotlin parser must pass all of them.
- `testdata/prototype-backup-sample.json`: prototype backup that the app must be able to restore.
- `testdata/import-sample.txt`: sample for text-file import.

## Stack and conventions

- Kotlin, Jetpack Compose, Material 3 (custom monochrome theme), single activity, Navigation Compose, ViewModel + StateFlow, Room, DataStore. Details are in `docs/PLAN.md`.
- Keep `domain/*` pure Kotlin, with no Android imports, and unit-test it on the JVM.
- **No `INTERNET` permission.** No analytics, crash reporting, ads or cloud SDKs. File access goes only through the Storage Access Framework.
- **No sample-data or debug buttons in the app UI**, including the prototype's "Generate 2 years of random training". Test data generators belong in test source sets only.
- **Never lose user data.** Room schema changes need a tested migration. Destructive actions need confirmation or Undo. Real-time backup must keep working.
- **UI:** Journals is always black and shows no calculations. Progress is always landscape. Keep text sizes, spacing and the black-and-white palette consistent with the prototype.
- Use `java.time` with ISO weeks starting on Monday, and dates stored as `yyyy-MM-dd`. Weights are in kg, with a dot as the decimal separator in the entry format.
- Strings go in `res/values/strings.xml`. Write accessibility content descriptions for icons and chart bars.

## Commands

Run from the repository root, in Android Studio's terminal:

```bash
./gradlew assembleDebug          # build
./gradlew testDebugUnitTest      # JVM unit tests (parser golden tests, domain)
./gradlew lint                   # Android lint
./gradlew connectedDebugAndroidTest   # instrumented/UI tests (emulator or device attached)
```

## Git workflow

- `main` is always buildable. Do each phase step on a branch (`feature/…`, `fix/…`) and merge via pull request.
- Small, focused commits with imperative messages ("Add golden tests for entry parser").
- Never commit signing keys, `local.properties` or build output (see `.gitignore`).
- Update `docs/SPEC.md` or `docs/PLAN.md` in the same pull request when behaviour or the plan changes, once the owner has approved it.
