# Lift Journal — Development plan (Android)

Goal: a native Android app that matches the prototype (`prototype/lift-journal.html`, specified in `docs/SPEC.md`). It adds:

- real-time backup
- import from text files
- editing and deleting entries
- no sample-data buttons

Every step below is proposed by Claude Code and approved by the owner before it is implemented (see `CLAUDE.md`).

## Technical baseline

| Area | Choice | Why |
|---|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 with a custom monochrome theme | Standard modern Android stack, and fits the minimal UI |
| Structure | Single activity, Navigation Compose (3 destinations), ViewModel + `StateFlow` | Simple, testable |
| Storage | Room for entries and goals; DataStore (Preferences) for settings | On-device, with migrations |
| Domain | Pure Kotlin packages `parser`, `stats`, `goals`, `export`, `importer` with no Android dependencies | Unit-testable on the JVM |
| Charts | Custom Compose `Canvas` drawing | The prototype's charts have specific rules (zoomed scale, month separators) that chart libraries make awkward |
| Spreadsheet export | Hand-written `.xlsx` / `.ods` writer using `java.util.zip` (port of the prototype's `buildXlsx` / `buildOds`) | No heavy dependency like Apache POI |
| Files | Storage Access Framework only (`CreateDocument`, `OpenDocument`, `OpenDocumentTree`) | No storage permissions, works with any folder provider |
| Min / target SDK | minSdk 26 (Android 8.0, has `java.time`); targetSdk = latest stable | |
| Package | `com.example.liftjournal` (placeholder, rename before first release) | |
| Network | **No `INTERNET` permission.** No analytics, crash reporting or cloud SDKs | Local-only by design |
| DI | Manual constructor injection via a small `AppContainer` | Hilt is unnecessary at this size; can be added later |

Suggested package layout:

```
app/src/main/java/com/example/liftjournal/
  MainActivity.kt, LiftJournalApp.kt, AppContainer.kt
  ui/theme/        monochrome theme; Journals forces black
  ui/journals/     JournalsScreen, JournalsViewModel, GhostTextField
  ui/progress/     ProgressScreen, ProgressViewModel, charts/
  ui/settings/     GoalsSettingsScreen, SettingsViewModel
  data/            Room (EntryEntity, GoalEntity, DAOs), SettingsStore, Repository
  domain/parser/   EntryParser (golden-tested)
  domain/stats/    period aggregation, weekly top sets, zoomed scale maths
  domain/goals/    goal current-value and percentage
  domain/export/   XlsxWriter, OdsWriter, SheetsBuilder
  domain/backup/   BackupJson (read/write, prototype-compatible)
  domain/importer/ TextImportParser
  backup/          RealtimeBackupManager (SAF folder writes)
```

## Phases

Each phase ends with its acceptance checks passing, unit tests green, and a short demo note in the pull request.

### Phase 0 — Project setup

- Create the project in Android Studio (Empty Activity, Compose) inside this repo. Keep `docs/`, `prototype/` and `testdata/` at the root.
- Set up a monochrome Material 3 theme, the three-tab navigation shell (bottom bar in portrait, rail in landscape) and edge-to-edge.
- Optional: a GitHub Actions workflow that runs `./gradlew assembleDebug testDebugUnitTest lint` on pull requests.
- **Done when:** the app builds and runs on an emulator and a device, shows the three empty tabs, and CI (if added) is green.

### Phase 1 — Parser and domain maths

- Port `parseText`, `num`, `mkSet`, `eff` and `exVol` to `domain/parser` with the same rules as `SPEC.md` §4.
- Write a JUnit test that loads `testdata/parser-golden.json` and checks every case. Expected values include volumes with no bodyweight and with 80 kg.
- Port the aggregation helpers to `domain/stats`:
  - ISO weeks with Monday start
  - monthly and weekly totals
  - weekly top set (tie broken by reps)
  - all-time yearly averages
  - average set weight
  - the zoomed-scale calculation used by the Top set chart
- Port the goal current-value rules to `domain/goals`.
- **Done when:** all golden cases pass, and the stats and goal functions have their own unit tests, including edge cases such as an empty year, a single week, and only bodyweight sets.

### Phase 2 — Data layer

- Room: `EntryEntity(date PK, text, updatedAt)` and `GoalEntity`. DataStore holds theme, ghost text, bodyweight, backup folder URI and last backup time.
- A repository exposes `Flow`s, plus parsed views via an in-memory cache keyed by `(date, text)`.
- Schema export is on and every migration is tested. Data must never be lost on app update.
- **Done when:** repository tests pass with an in-memory database, and saving and restoring through process death works.

### Phase 3 — Journals (including edit and delete)

- Black screen, search field, month tags, and the entry list exactly as `SPEC.md` §3.
- Inline editing with autosave: debounce 500 ms, plus save on focus loss and on `ON_STOP`.
- Today's entry gets focus on open. The blinking cursor shows when it is empty.
- Tapping the date header picks another date.
- **Ghost text:** a custom `BasicTextField` decoration that draws the grey ghost line remainder after each typed line, plus the Tab-to-accept behaviour.
- **Edit and delete (new):**
  - Long-press menu with Change date and Delete.
  - Delete shows a snackbar with Undo.
  - Clearing an entry's text deletes it, with Undo.
  - Change date offers to merge when the target date already has an entry.
- **Done when:** a manual test pass on a phone covers typing, autosave across app kill, search, month filter, ghost text, change date, delete and undo. UI tests cover delete with undo and search.

### Phase 4 — Progress

- Landscape-only while the tab is active. Restore the previous orientation on leaving.
- Period control (year chips, All-time), header stats, tag row, exercise and goal chips.
- Canvas charts:
  - zero-based month bars
  - weekly top-set bars with month separators, zoomed scale and short bars for load-off weeks
  - all-time yearly averages
  - tap to select, with dimming and readout
  - table toggle
- Goal view: large centred percentage on the left, Now vs Goal bars on the right.
- Bodyweight-missing hint.
- **Done when:** numbers match the prototype for the same data. Import `testdata/prototype-backup-sample.json` or a larger backup into both, then compare the readouts. Charts are readable at 360 dp height and in dark theme.

### Phase 5 — Goals & Settings

- Goals list and form, bodyweight, ghost text toggle, appearance, entry-format help, erase all data (with confirmation).
- Spreadsheet export (.xlsx / .ods) through `CreateDocument`. Port the prototype writers and test them by re-opening the files in a JVM unit test (zip structure plus XML well-formed).
- Restore from JSON backup, accepting prototype v1 and v2 files, with a preview before replacing.
- **No "Generate random training" or any other sample-data button.** If test data is needed, a generator may live in `app/src/test` or `app/src/androidTest` only, never in `main` or debug UI.
- **Done when:** exported files open in Excel, Google Sheets and LibreOffice, and restoring the sample backup reproduces the same entries, goals and bodyweight.

### Phase 6 — Real-time backup (new)

- **Setup:** in Goals & Settings, "Choose backup folder" (`OpenDocumentTree`). Take a persistable URI permission. Suggest creating a `LiftJournal` folder in Documents.
- **When it writes:** after every committed change (entry save or delete, goal change, settings change), debounced about 2 s. It also flushes on `ON_STOP`.
  - Use a WorkManager unique one-time work (`REPLACE` policy) so writes survive the app being closed.
- **What it writes:**
  - `lift-journal-backup.json`: the full backup, in the same format as manual backup (`SPEC.md` §7.1). Write a temporary document first and then replace, so a crash never leaves a half-written file. If the provider doesn't support rename, fall back to truncate-and-write.
  - Daily snapshot `lift-journal-YYYY-MM-DD.json`, keeping the 14 newest.
- **Status and errors:**
  - Settings shows "Last backup: today 12:04" and a "Back up now" button.
  - If the folder or its permission disappears, show a warning in Settings and a one-time snackbar on Journals. Never block logging.
- **Restore:** via phase 5's restore. Point the user at the folder.
- **Note on cloud folders:** the app itself has no cloud access. A cloud copy needs a folder that some other app syncs. Check on a real device which providers support folder (tree) access; some, including Google Drive's Android app, may not. Document what works in the README.
- **Done when:**
  - Editing an entry updates the backup file within about 3 s.
  - Killing the app mid-edit still leaves a valid backup.
  - Revoking the folder permission shows the warning.
  - Restoring the backup on a fresh install gives identical data.

### Phase 7 — Import from text files (new)

- In Goals & Settings, "Import from text file" (`OpenDocument`, MIME `text/plain` and `text/markdown`). A typical source is a Google Docs file downloaded as plain text (*File → Download → Plain text (.txt)*). No cloud access is involved.
- **Format:**
  - A line that consists only of a date (optionally with a weekday word) starts a new entry. Everything up to the next date line becomes that entry's text.
  - Accepted dates: `9.11.2026`, `09.11.2026`, `9.11.26`, `2026-11-09`, with an optional weekday prefix in English or Finnish (`Mon 9.11.2026`, `ma 9.11.2026`).
  - Lines before the first date are ignored and listed in the preview.
  - Encoding: UTF-8 (with or without BOM). If decoding fails, fall back to Windows-1252.
- **Preview before importing:**
  - Number of entries found and their date range.
  - Dates that already have entries.
  - The number of lines that can't be read as sets (they stay as notes).
- **Conflicts:**
  - Default: append the imported text below the existing text.
  - Skip any entry whose text is already identical.
  - Never delete anything.
  - The user can switch the conflict choice to "Skip existing dates".
- Import is one atomic database transaction, followed by a real-time backup.
- Sample file: `testdata/import-sample.txt`.
- Later, optional: `.docx` import (Google Docs → *Download → Microsoft Word*) by reading `word/document.xml` paragraphs from the zip.
- **Done when:** importing the sample gives 4 entries with the correct dates and parsed sets, re-importing the same file changes nothing, and unit tests cover each date format and the conflict rules.

### Phase 8 — Polish and release readiness

- Accessibility: TalkBack labels for chart bars (spoken values), touch targets of at least 48 dp, font scaling up to 200% in Journals and Settings.
- Performance: 5 years of data (about 800 entries) scrolls and charts render without jank. Use baseline profiles if needed.
- App icon, release build with R8, signing config kept out of git, version naming.
- Final parity pass against the prototype.

## Open questions for the owner

1. **Unreadable sets in Journals.** The prototype silently ignores text it can't read as sets. Should the app show a subtle marker, for example an underline, on unreadable parts?
2. **Bodyweight history.** Bodyweight is currently one value applied to all history. Should it become a dated log, so old bodyweight sets keep the bodyweight they had?
3. **Volume goals.** They count the current week or month so far, starting from 0 again each period. Should there be an option to compare against the last full week or month instead?
4. **Android Auto Backup** (to the Google account): off by default, since backup goes to the chosen folder. Enable it as an extra safety net?
5. **Units.** kg only, or add an lb option?
