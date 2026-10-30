# Home / Explorer batch integration review and acceptance gate

**Branch:** `feature/post-brand-home-polish` (local only)
**Parent:** `feature/modern-paper-ink-foundation` / PR #8 head `e181918c4a24f7e5287707434cc4c448921c0e7f`
**Review status:** Source review and static dry-runs only. No local Android build, Kotlin execution, APK, or screenshots.

## Findings resolved in this local review

| Priority | Finding | Concrete case | Minimal correction |
|---|---|---|---|
| Must fix | Missing Compose `heightIn` import in `ExplorerLayouts.kt` | The responsive batch added `heightIn` calls in list headers and column rows. The file imported `height` but not `heightIn`; Kotlin compilation would fail. | Add the existing `androidx.compose.foundation.layout.heightIn` import |
| Should fix | Sortable list column heads retained a small hit area | Row grew to 48dp, but only the label-sized inner `ColumnHead` was clickable. On a tablet, tapping blank vertical space within the header did not trigger sorting. | Apply `heightIn(min = 48.dp)` to the clickable `ColumnHead` row |
| Should fix | File-kind filter recovery was hidden in an entirely empty folder | With an empty folder and an active PDF/Note/Canvas filter, `source.isEmpty()` showed New Note/New Canvas/Import rather than the existing `All Kinds` recovery action. | Only show the create-actions empty state when `kindFilter == null`; let the existing filtered-empty branch supply `All Kinds` |

## Reviewed behavior and boundaries

- **Home/Explorer:** empty-folder creation hierarchy, search/colour/kind/Recent recovery, sidebar navigation and original file callbacks.
- **Tiles:** keyboard focus, selection semantics, file cards and folder chips across Grid, List, Gallery, Timeline, Pinned/Recent and Columns. Existing drag hit-testing and file mutation functions are unchanged.
- **Sizing:** measured Explorer pane widths for Grid/Gallery, List metadata visibility under larger text, column/path preview balance, compact header search width and first-run folder selection.
- **Tests:** `ExplorerLayoutSizingTest.kt` covers key widths and tile settings; JUnit tests are created but **not executed on Windows**.
- **No changes** to note storage, canvas ink algorithms, PDFs, database, network, credentials, native libraries, package IDs, third-party notice or audit closure status.
- **Still unverified on hardware:** accessibility action merging, actual TalkBack focus/selection announcements, 2x text rendering, split-screen constraints, stylus long-press drag and preview behavior.

## Acceptance checklist for the batched GitHub checkpoint

1. **Local review gate (before push):** `git diff --check`, `tools/ci/check_project.py`, backup privacy validation, Android resource/locale checks and focused source assertions for import/filter/header/layout wiring. Leave untracked `.kotlin/` untouched.
2. **Branch integration gate:** PR #8 must remain untouched by this local batch. After it passes review and is merged, rebase/cherry-pick *only* this branch's post-PR #8 commits onto the resulting `main` SHA. Review the resulting diff: it must not reintroduce prior branding or CI policy commits. Preserve a local backup before any history rewrite.
3. **One publishing checkpoint:** Push `feature/post-brand-home-polish` as one reviewed batch and create its own PR to `main`. Do not push individual local commits or run an extra expensive CI build for every change. Keep separate `chore/codeql-compile-only` outside this UI PR.
4. **GitHub source + integration gate:** On the **exact PR head SHA**, run Kotlin unit tests (including `ExplorerLayoutSizingTest`), Android lint, debug APK build and APK verifier. Inspect the actual workflow jobs/logs and artifact. Failed or missing checks block merge.
5. **Device QA:** Phone 280/320/360dp, tablet width, narrow multi-window, landscape/portrait, 1x/1.3x/2x font, Light/Dark/OLED. Exercise all Explorer layouts, search open/close, sort headings, filter reset, first-run storage choice, selection, keyboard arrows and TalkBack announcements. Verify stylus long-press, drag-and-drop and file persistence with disposable test notes.
6. **Merge/release:** Only after cloud CI **and** applicable device acceptance. Do not mark any of the 45 historical XNotes audit findings closed from a source-only improvement.

**Current state:** Reviewed and corrected locally; safe to continue other independent source work. No push/merge/release requested for this checkpoint.
