# Lift Journal — Product specification

This describes what the Android app does. It is derived from the working prototype (`prototype/lift-journal.html`) plus the decisions recorded in `docs/PLAN.md`. Where this document says *(new)*, the feature is not in the prototype yet and is specified here for the first time.

## 1. Principles

- **Fast logging.** Opening the app puts the cursor in today's entry. A set is typed as plain text: `deadlift 50 x 5`.
- **Minimal black-and-white UI.** No colour accents. Journals is always black. Progress and Goals & Settings follow the theme setting (system, light or dark).
- **Local only.** No account and no network access (no `INTERNET` permission). Data lives on the device and is backed up in real time to a folder the user picks.
- **Calculations stay out of the journal.** Journals shows only dates and what the user wrote. Numbers live in Progress.

## 2. Navigation

Three tabs, in this order:

1. **Journals** (start screen)
2. **Progress** (always shown in landscape)
3. **Goals & Settings**

In portrait the tabs are a bottom bar. In landscape they are a navigation rail on the left.

## 3. Journals

### 3.1 Layout

- The screen is black. The only visible control is a search field at the top: rounded, dark grey, with a magnifier icon and the placeholder "Search".
- Below it is a list of journal entries, separated by thin lines. Each entry shows:
  - its date as the header, formatted `dd.mm.yyyy` (e.g. `09.11.2026`);
  - its text, editable in place.
- The **top entry is today's**. When it's empty it shows only a blinking text cursor (`|`), plus the ghost text if that's enabled (3.4).
- Earlier entries follow, newest first.
- There are no totals, volumes, labels or other calculations anywhere on this screen.

### 3.2 Search

- Tapping the search field shows **month tags** below it: one per month that has entries, newest first, labelled `Nov 2026`.
- Tapping a tag shows only that month's entries. Tapping it again removes the filter.
- Typed text filters entries whose text or `dd.mm.yyyy` date contains every typed word, case-insensitive.
- While a query or month filter is active, today's empty entry is hidden. A × button in the search field clears the query and the filter and closes the tags.

### 3.3 Writing, editing and deleting

- Typing saves automatically about 500 ms after the last keystroke. It also saves when the field loses focus and when the app goes to the background.
- Every entry can be edited in place.
- Tapping the date of the top entry opens a date picker. This lets the user write an entry for a past day that has none.
- *(new)* **Delete:** long-pressing an entry opens a menu with "Change date" and "Delete".
  - Delete removes the entry and shows a snackbar "Entry deleted · Undo" for about 5 seconds.
  - Clearing all of an entry's text also deletes it when the field loses focus, with the same Undo.
- *(new)* **Change date** moves an entry to another date. If that date already has an entry, ask whether to merge: the moved text is appended below the existing text.
- One entry per date. The date is the identity.

### 3.4 Ghost text (suggestions)

- Today's entry shows the most recent earlier entry with the **same weekday**, within the last 8 weeks (normally last week), as grey text in the input field itself.
  - There is no box, label, date or button.
  - Only lines containing a digit are used, so notes are left out.
- As the user types, each typed character covers one ghost character in the same line. The ghost text after the typed part stays visible.
- With a hardware keyboard, Tab fills in the rest of the current line, if what's typed so far is a case-insensitive prefix of the ghost line.
- Ghost text can be turned off in Goals & Settings.

## 4. Entry format

Golden test cases: `testdata/parser-golden.json`. The Kotlin parser must reproduce them exactly.

| Input | Meaning |
|---|---|
| `deadlift 50 x 5` | 1 set, 50 kg × 5 = 250 kg volume |
| `deadlift 50 kg x 5, 5, 5` | 3 sets of 5 at 50 kg. `kg` is optional, and `x`, `×` and `*` are accepted. |
| `squat 100 x 5, 5, 110 x 3` | Weight change mid-exercise |
| `110 x 3` on the next line | Continues the previous exercise |
| `deadlift 75 x 5, bench press 60 x 5` | Several exercises on one line |
| `squat (60 x 10), 100 x 5` | Anything in parentheses is **not recorded** |
| `(bench 80 x 3)` | A whole line in parentheses is not recorded |
| `pull-up bw x 8, 8` | Bodyweight set |
| `push-up 0.5 bw x 20`, `dips 1/2 bw x 10`, `chin-up 1.25bw x 5` | Bodyweight multiplier |
| `felt strong` | A line without digits is a note: kept in the text, not counted |

Rules:

- **Exercise names** are matched case-insensitively, with repeated spaces collapsed. They're displayed in the first spelling seen, with the first letter capitalised. Finnish letters (ä, ö, å) are allowed.
- **Decimals** use a dot (`72.5`), because commas separate sets.
- **Unreadable tokens** (e.g. `7O x 5`) are collected as errors. The text is still saved. The journal doesn't show errors (see 3.1); see `PLAN.md` for an open question.
- **Volume** = weight × reps.
- **Bodyweight sets** use *multiplier × bodyweight* from Goals & Settings. When no bodyweight is set, bodyweight sets are left out of all Progress figures, goals and export volumes. Progress then shows the hint: "Bodyweight sets (bw x reps) aren't counted. Set your bodyweight in Goals & Settings."
- **Workout:** a date whose entry has at least one recorded set.

## 5. Progress

### 5.1 Orientation and layout

Progress is always landscape (`SCREEN_ORIENTATION_SENSOR_LANDSCAPE` while the tab is shown). The layout, top to bottom:

- **Header row:** the title "PROGRESS", the period control, and three stats on the right.

  | Period | Stats |
  |---|---|
  | Year | workouts · kg in year · kg this month (current year) or average kg / month (past years) |
  | All-time | workouts · kg all-time · average kg / month |

- **Chart title**
- **Average set weight line** (Top set view only)
- **Readout line**
- **"Show table / Show chart" toggle**
- **Chart**, filling the remaining height
- **Tag row:** `Goal` (only when at least one goal exists, always first), `Total volume / month`, `Top set / week`, `Exercise volume / month`
- **Exercise chips** for exercise views (weighted exercises in the period, sorted by volume). In the Goal view, goal chips are shown instead when there is more than one goal.

### 5.2 Period

- `All-time | <year> ▾` segmented control.
- Tapping the year button shows chips for each year that has data. Picking one switches to it.
- The default is the current year.
- The period control is hidden in the Goal view.

### 5.3 Year views

- **Total volume / month:** bars January → current month (or December for past years), zero-based. The current month's label is bold.
- **Exercise volume / month:** the same, for the selected exercise.
- **Top set / week:** one bar per week, from the ISO week containing 1 January up to the current week.
  - Each bar is the heaviest set that week; ties are broken by more reps.
  - Weeks start on Monday and are labelled with their ISO week number.
  - Thin vertical lines separate months, with the month name at the start of each.
  - Bars are wide, with about a 2 px gap.
  - **Zoomed scale from zero:**
    - The axis shows `0` at the bottom.
    - A short bottom section (about 18 px) represents 0 → *lo*, marked with a small `//` on the axis.
    - The rest of the height is *lo* → *max*.
    - Gridline step is the smallest of 2.5, 5, 10, 20, 25, 50, 100 or 250 kg that fits about one label per 14 px.
    - *lo* is a step below the lightest "normal" week. Normal means at least 80% of the median top set.
    - Lighter weeks (load-off weeks) show as short bars up to the bottom section only.
    - Bars are solid with no cut in them.
  - The **average set weight** line under the title reads "Average set weight 94.2 kg · 202 sets in 2026": the mean of all recorded set weights for the exercise in the period.

### 5.4 All-time views (averages only)

One bar per year. The first year counts from its first logged month, and the current year counts up to the current month.

- **Total volume:** average volume per month in that year.
- **Exercise volume:** the exercise's average volume per month.
- **Top set:** average weekly top set, over the weeks the exercise was trained. It uses the same zoomed scale.

### 5.5 Interaction

- Tapping a bar selects it: the other bars dim, its value is labelled, and the readout shows details such as "October: 41,230 kg · 12 workouts" or "Week 43 · 19–25 Oct: 130 kg × 5 on Thu 22 Oct". Tapping it again clears the selection.
- With nothing selected, the readout shows a summary (period total, best and latest top set, or all-time average) and the tallest bar is labelled.
- The table toggle replaces the chart with the same data as a table.

### 5.6 Goal view

- **Left half:** the percentage of goal in very large type, centred, with "OF GOAL" under it.
- **Right half:** a bar chart comparing a solid **Now** bar against an outlined **Goal** bar, with a dashed line at the target. It is zero-based.
- **Title:** "Goal · Squat top set 160 kg".
- **Lines under the title:** "Now 145 kg · heaviest set in the last 4 weeks", then "15 kg to go" or "Goal reached".

## 6. Goals & Settings

Sections, top to bottom:

1. **Goals**
   - The list of goals, each with its current value, % of goal, and × to remove it.
   - Below the list is a form:
     - **Goal type:** Top set weight, Average set weight, Total volume, or Exercise volume.
     - **Exercise:** for every type except Total volume.
     - **Period:** per week or per month, for volume types only.
     - **Target (kg):** the placeholder suggests about 10% above the current level.
     - **Set goal** button.

   | Goal type | "Now" means |
   |---|---|
   | Top set weight | Heaviest set in the last 28 days |
   | Average set weight | Mean set weight in the last 28 days |
   | Total / exercise volume | Volume in the current week (Monday start) or current month so far |

   Percentage = now ÷ target × 100, rounded. It can exceed 100%.
2. **Bodyweight:** a single number in kg, which can be cleared. Changing it recalculates all history.
3. *(new)* **Backup folder:** see `PLAN.md` phase 6.
   - Shows the chosen folder, the last successful backup time, and buttons "Choose folder" and "Back up now".
   - If the folder permission is lost, a warning row says so.
4. **Spreadsheet export:** "Export .xlsx" and "Export .ods", saved via the system file picker. See 6.1.
5. **Restore from backup:** pick a JSON backup, see a preview, then confirm "Replace all data". Backups from the prototype must be accepted.
6. *(new)* **Import from text file:** see `PLAN.md` phase 7.
7. **Journal ghost text:** On / Off.
8. **Appearance (Progress & Settings):** System, Light or Dark.
9. **Entry format:** a short help card with the examples from section 4.
10. **Erase all data:** requires confirmation.

**Not in the app:** the prototype's "Generate 2 years of random training" button, and any other sample-data or debug buttons, in any build.

### 6.1 Spreadsheet export (same as prototype)

Six sheets. Header row is bold and frozen. Numbers are written as numbers.

| Sheet | Columns |
|---|---|
| Sets | Date, Weekday, ISO year, ISO week, Exercise, Set, Weight (kg), Reps, Volume (kg), Bodyweight × |
| Daily volume | Date, Weekday, ISO week, Exercise, Sets, Reps, Volume (kg), Top set (kg) |
| Weekly volume | ISO year, ISO week, Week starts (Mon), Exercise, Sets, Reps, Volume (kg), Top set (kg) |
| Monthly volume | Month, Exercise, Sets, Reps, Volume (kg), Top set (kg) |
| Monthly total | Month, Workouts, Sets, Total volume (kg) |
| Journal text | Date, Weekday, Day volume (kg), Entry text |

Bodyweight sets without a bodyweight set have empty weight and volume cells.

## 7. Data model

- **Entry:** `date` (ISO `yyyy-mm-dd`, unique), `text`, `updatedAt`. The parsed sets are derived from `text` and never edited directly. The derived form may be cached, but must be re-derivable.
- **Goal:** `id`, `type` (`top` / `avg` / `vol` / `exvol`), `exerciseKey` (normalised name, or null), `period` (`week` / `month`, or null), `targetKg`.
- **Settings:** `theme`, `ghostText` (boolean), `bodyweightKg` (nullable), `backupFolderUri` (nullable), `lastBackupAt`.

### 7.1 Backup JSON (compatible with the prototype)

```json
{ "v": 2,
  "entries": [ { "date": "2026-11-02", "text": "squat 120 x 5, 5, 5" } ],
  "settings": { "theme": "system", "ghost": true, "bodyweight": 80,
                "goals": [ { "type": "top", "ex": "squat", "per": null, "target": 140 } ] } }
```

- Prototype backups also contain a derived `ex` array per entry. Ignore it on restore and re-parse `text`.
- Version 1 prototype backups (`liftjournal.v1`: `entries[].logged`, `logText`, `notes`) are converted the same way the prototype's `migrate()` does.
- Sample file: `testdata/prototype-backup-sample.json`.
