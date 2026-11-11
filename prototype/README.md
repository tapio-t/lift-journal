# Prototype

`lift-journal.html` is the working prototype that the Android app is built from. It is a single self-contained HTML file with no dependencies and no network access.

**Open it:** download the file and open it in Chrome (desktop or Android). On a desktop browser it shows the app inside a phone frame; Progress turns the phone sideways.

**What it is for**

- The reference for behaviour, layout and calculations. When `docs/SPEC.md` and the prototype disagree, raise it with the project owner rather than picking one.
- The source of `testdata/parser-golden.json` (generated from its `parseText()` and `exVol()` functions).
- Its backup format (`Save backup` → JSON) must be restorable by the Android app; see `testdata/prototype-backup-sample.json`.

**Prototype-only features that must not be ported**

- The **Generate 2 years of random training** button in Goals & Settings. The Android app has no sample-data buttons in any build.
- Phone-frame rendering, CSS rotation of the Progress view and the browser full-screen/orientation-lock workaround. The Android app uses a real landscape orientation instead.
- `localStorage` persistence. The Android app uses an on-device database plus real-time folder backup.

**Useful code locations inside the file** (search for these function names)

| Area | Functions |
|---|---|
| Entry parsing | `parseText`, `num`, `mkSet`, `eff`, `exVol` |
| Journals | `listHtml`, `entryHtml`, `ghostFor`, `renderGhost`, `acceptGhost`, `commitNow` |
| Progress data | `periodData`, `chartSpec`, `readout`, `avgLine` |
| Charts | `barSvg` (zoomed weekly top-set scale), `goalSvg`, `drawGoal` |
| Goals | `goalCurrent`, `goalPct`, `goalName`, `goalsHtml` |
| Export | `sheetsData`, `buildXlsx`, `buildOds`, `zipStore` |
| Backup / restore | `backup` and `restore` click handlers, `migrate` |
