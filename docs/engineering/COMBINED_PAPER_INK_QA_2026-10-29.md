# Combined Paper & Ink + accessibility preview — device QA

**Purpose:** One disposable Android debug APK for checking integration across Paper & Ink and three follow-up UI batches. This is a **QA-only branch**, not a release or approval to merge the original PRs.

## Source provenance

- Integration branch: `qa/combined-paper-ink-accessibility`.
- Based on `main` after PR #10, commit `394e23a` (automatic CI on every batch push).
- Includes original unchanged heads: PR #8 `e181918` (brand, dialogs and theme); PR #9 `1b63567` (Home/Explorer); PR #11 `bda4cea` (Settings/toolbar); PR #12 `60cfd3f` (Editor, shapes and search).
- Merged only into the QA branch with explicit merge commits. Two conflicts occurred in `docs/canvas/BRAND_UI_AUDIT_MATRIX.md`; both were resolved by retaining evidence from all branches. No application-source conflict occurred.
- Neither the original PR branches nor `main` receives these combined UI changes. Do **not merge the QA branch into `main`**; the original PRs retain their own device-review and integration gates.

## Download the exact preview

1. Open GitHub **Actions → Android CI**, select the run whose branch is `qa/combined-paper-ink-accessibility` and confirm its head commit.
2. After the **Source, resources and attribution** and **JDK 17 / tests / lint / debug APK** jobs succeed, download the `NotesTakingApp-debug-...` artifact from **Artifacts**.
3. Extract the ZIP and install its **debug** APK on a compatible Android test device. It is **not a production-signed release** and should never be distributed as one. Do not test on a device containing irreplaceable notes; use backups and disposable documents.
4. If GitHub tests/lint/APK verification fail, stop and record the failing job/step. Do not claim an installable APK until the run and artifact are verified.

## Physical acceptance checklist

| Area | Test on phone and tablet | Acceptance evidence |
| --- | --- | --- |
| First launch | App icon, splash, logo/loader and light/dark theme | Screenshots; no crash or blank screen |
| Home and Explorer | New Note, New Canvas, Import PDF, create folder, Search, Kind/Color filters, reset filters, Grid/Gallery/List and empty recovery | Actions work; no clipping or inaccessible controls |
| Window scaling | 280–360dp pane widths, tablet landscape/split-screen and 100%/200% system font | No unusable layout, lost actions or overlapping text |
| Settings | System/light/dark/OLED/custom palette preview, Cancel/Back/reset protection | Preferences remain until deliberate reset; note data preserved |
| Toolbar customizer | Tool visibility toggles, section drag/reorder, delete/add a section | TalkBack announces state and action; drag and save work |
| PDF and zoom | Import/open disposable PDF, contrast/invert/brightness/sepia, min/max zoom including equal bounds | +/- buttons stop at bounds and never appear active when no change is possible |
| Drawing / tables | Draw, erase, select a shape, change side/point counts, insert/restyle table and Undo/Redo | Geometry, table size and save/reopen are correct |
| Text editing | Type, adjust text size 6–96pt, Done, rich-text formatting, narrow-pane/right-edge floating toolbar | Done remains reachable via scrolling and existing text survives |
| Search and focus | Clear query, Aa/Whole Word, Previous/Next and current result, Enter/Shift+Enter | Correct result/page, accessible selection states |
| Stylus / accessibility | Stylus strokes with palm contact, TalkBack labels/disabled states, hardware keyboard | No unexpected stroke loss, crashes or focus traps |
| Data safety | Save, close and reopen a disposable note, import/export a disposable PDF | No loss or unexpected overwrite of existing notes |

Record **PASS/FAIL/NOT TESTED** for each area, exact device model, Android version, APK run/commit, reproducible steps, and screenshots without private user content.

## Gates that remain

- An integrated preview APK validates that these branches coexist; it **does not** replace QA for the exact head of each underlying PR or erase the 45 historical audit findings.
- PR #8 requires first-party phone/tablet acceptance before merge. Once accepted and merged, retarget/cherry-pick only each follow-up's incremental commits onto current `main`, run CI again on exact updated SHA and review remaining device risks.
- Never run Gradle, Kotlin tests, lint, native compilation, APK build or installation on this Windows development machine; GitHub Actions provides executable validation.
