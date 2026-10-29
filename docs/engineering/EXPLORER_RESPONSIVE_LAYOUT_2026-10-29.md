# Explorer responsive layouts — local batch (2026-10-29)

**Branch:** `feature/post-brand-home-polish`
**Status:** Local source change only; not published. No local Android/Gradle build or executable test has run.

## Problem and implementation

- Grid columns previously used `LocalConfiguration.screenWidthDp`, which included width occupied by the persistent rail/sidebar. On resized or split-screen tablet windows, too many columns could be placed inside a narrower Explorer pane. Both grid and gallery now compute columns inside their own `BoxWithConstraints` using actual pane `maxWidth`.
- Grid spacing and column sizing share the same 12dp gap. The chosen tile-size setting remains unchanged, and column counts clamp to 1–10.
- The compact search pill now subtracts the header's fixed controls and clamps to its 40dp collapsed size rather than forcing 160dp into a narrower header. Search query and focus callbacks are unchanged.
- The initial **Choose folder / Use app storage** empty state reuses the existing `EmptyPane`. Its actions wrap to additional rows and its content scrolls on short landscape or high-text-scale windows. Storage callbacks stay unchanged.
- List and column rows now use **minimum** heights so large text can grow vertically. The list's extra Kind/Pages/Size/Modified columns only appear when the measured width can accommodate the system font scale. Column labels use ellipsis when required.
- The columns view now reserves at least half its available width for a preview in narrow panes (capped at the original 360dp on larger widths), preventing the preview from shrinking into an unusably thin strip.
- New JUnit source tests in `app/src/test/java/com/notestaking/app/ui/ExplorerLayoutSizingTest.kt` assert phone/split-pane/grid/gallery and max-column sizing. These tests are **written but not executed** locally.

## Non-goals

- No file-query, move, rename, import, delete, registration, note format, storage/privacy, or editor drawing logic changed.
- No new dependency, font, icon, or Android manifest change.
- UI-19/20/21 remain **partial source fixes**, not device-accepted closures.

## Device QA checklist (required after GitHub CI)

| Window / text | Expected UI behavior |
|---|---|
| 280dp phone, 1x | Grid has one readable tile (M); search control remains inside header; choose-storage actions stack and scroll |
| 320–360dp phone, 2x | Explorer search, empty states and both storage options remain discoverable; no horizontal overflow |
| 520dp split-screen pane, 1x | M grid shows two columns; file labels and selection remain accessible |
| 652dp Explorer pane, 1x | L grid shows two full-size tiles instead of squeezing into extra columns |
| 780dp Explorer pane, 1x | M grid shows three columns; scrollbar and toolbar remain aligned |
| 840dp Explorer pane, 1x | M gallery shows four usable page cards |
| Tablet 800–1200dp, 1.3x/2x | List may hide extra fixed-width metadata columns when text is enlarged; rows and column headers grow rather than clip |
| Tablet narrow multi-window | Columns preview retains meaningful width; horizontal path scrolling, preview and keyboard focus still work |
| Portrait/landscape, TalkBack, keyboard | No inaccessible action, swallowed back press, cut-off file title or broken drag-to-folder |

Also verify during pinch-to-resize: tile size setting remains consistent, the grid recomputes as pane width changes, and file-selection state is preserved.

## Acceptance evidence

- Local static source/resource and XML checks plus `git diff --check` must pass.
- GitHub must compile Kotlin, run the JUnit suite, lint and build/verify the debug APK on the **exact pushed batch SHA**.
- At least phone + Android tablet screenshots, light/dark themes, 100% and 200% font scale, portrait/landscape and split-screen should be reviewed before claiming responsive visual acceptance. No screenshot or APK was produced locally.

**Publishing rule:** keep this UI work on the unpushed local batch until a deliberate batched GitHub checkpoint; do not modify PR #8's published head as a side effect.
