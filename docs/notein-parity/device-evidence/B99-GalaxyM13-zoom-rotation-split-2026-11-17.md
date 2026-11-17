# B99 Galaxy M13 — PDF navigation, fit, rotation and split selection test

**Test date:** 2026-11-17 (IST)

**Scope:** Real-phone follow-up smoke checks; **does not satisfy all NT-043–050 or REL-11/12/16 acceptance**.

**Reference:** [Next APK update plan](../../github/NEXT_UNIVERSAL_APK_UPDATE_PLAN_2026-11-17.md), [B100 UI issue #28](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/28), and [B99 selection-action issue #29](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/29).

## Exact build and safe fixtures

- **Phone:** Samsung Galaxy M13 (`SM-M135F`), Android 14, 1080x2408 px, CPU ABI `armeabi-v7a`.
- **Verified installed APK:** B99 `b299cbb8972267eebe103493fa093f7e5cfa6f70`, debug package `com.notestaking.app.debug`, exact on-device base APK SHA-256 `25eaf748dbe1c2039c66aa383f7ecc42bd9de2aeb3043dbc47f24fcb3b248025`.
- **QA-only imported note:** `QA_Imported_B99_20261116` with two A4 PDF pages and green pen annotations.
- **Immutable source PDF check:** `/sdcard/Download/QA_NotesTaking_B99_DeviceTest_20261116.pdf`, SHA-256 `633905d39cd8bf82267430b865ddb63d8851ac3c8653fac9e680f6c46faca083`, verified after rotation/reopen.
- **Other QA-only notes:** `QA_GalaxyM13_20261116`, `QA_Canvas_M13_20261116`.
- No personal documents were opened or modified. No APK install, permission reset, factory reset, production signing, or local Gradle run occurred.

## Device observations

| Scenario | Evidence | Result |
|---|---|---|
| Open test PDF in Read mode | Chosen from library by exact QA filename. Read/Annotate toggle and sidebar showed two pages. | **PASS, smoke** |
| Previous / Next | With sidebar closed, bottom page counter changed **`2 / 2 → 1 / 2 → 2 / 2`** on exact Prev/Next buttons, and rendered viewport showed >11k changed pixels between page screenshots. | **PASS, smoke** |
| Fit whole page / width / height | `Fit` menu exposed three distinct options. Width and whole-page produced no measured change for this fixture; switching width→height changed 7,450 viewport pixels; height→whole changed 7,450; reapplying whole matched original (0 changed pixels). | **PASS for control and visible fit-height; other fit geometry inconclusive** |
| Rotate Page 2 | Selected Page 2's `Page options → Rotate… → Right`. Four 90° turns were accepted; the selected thumbnail alternated **portrait → landscape → portrait → landscape → portrait**. After the fourth turn and a panel scroll, the Page 2 preview bounds were back to a tall portrait thumbnail `[104,1427][526,2024]` (422x597), same shape as before rotation. | **PASS for UI rotation and orientation restoration; precise ink landmark anchoring across every angle NOT VERIFIED** |
| Reopen after rotation | Switched to Annotate, returned to library via Home, reopened `QA_Imported_B99_20261116`. Green ink remained visible on both pages, and the exact original exported PDF SHA-256 was unchanged. | **PASS, smoke** |
| Selection-mode Split | Long-pressed QA imported PDF, scrolled and tapped `QA_GalaxyM13_20261116`. The selection bar displayed enabled `Open side by side`; ADB tapped button center twice (`469,316`) and alternate clickable-parent point (`515,316`). The selection remained active and no editor panes opened. | **UNRESOLVED / FAIL for injected selection-bar tap path**, not yet proof the split engine is broken |
| Clear Selection tap | ADB tapped `Clear selection` (`124,315`) and selection remained active. Android Back **did** exit selection. | **UNRESOLVED / FAIL for injected selection-bar tap path** |
| Differential touch behavior | Ordinary library `View options` opened and closed on ADB taps; list swipe scrolled, confirming basic injected input works. Search tap was inconclusive in the same session. | **Partial control pass; root cause undetermined** |
| Stability | The test app remained in `MainActivity`, with no observed app crash during these interactions. Cold restart used to diagnose touch focus once, `Status: ok`, `TotalTime: 5548ms` (one sample only). | **PASS for process survival; NOT a performance benchmark** |

### Interpretation of the selection-bar observation

The selection toolbar child exposes `Open side by side` with `enabled=true`, and its outer parent reports `clickable=true enabled=true` at `[401,246][536,381]`. Taps within those bounds did not trigger the expected action. This was repeated before a restart, but **physical finger-touch confirmation is still needed**. A purple `New Note` / `Export PDF` pill was visible over the library's upper-left title, matching the user's B100 screenshot concern. `Tooltip.kt` uses a popup with `FLAG_NOT_TOUCHABLE`; **do not assume that tooltip is the cause without additional investigation**.

Track as [GitHub issue #29](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/29), linked to B100 [issue #28](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/28). In the next update, validate hitboxes and manual finger accessibility before concluding split-pane behavior is broken. **No source patch or CI run is part of this test.**

### Remaining test requirements

1. Precise 25–400% zoom and a landmark-alignment fixture with multiple zoom levels and rotations; ADB single-finger commands did **not** generate a real two-finger pinch.
2. Independent split-pane navigation/active-pane/undo once the selection toolbar responds, plus narrow 180/240/320dp bounds.
3. 200-page PDF/performance, malformed cloud/provider handling, actual TalkBack and physical keyboard, phone/tablet layouts and S Pen hardware acceptance.
4. A **new** B100 APK containing the requested library button, top filename and chevron drawing toolbar must be built and tested later. The current B99 APK predates these requirements.
5. All original 79 NT acceptance cases and REL-08–REL-20 remain governed by their complete individual criteria; this smoke log does not make them `verified`.

**Final UI state:** Library on the connected phone, selection cleared using Android Back, View Options closed. All three disposable QA files are retained. No original PDF data was overwritten.
