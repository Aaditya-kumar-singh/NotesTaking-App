# NotesTaking B99 — Samsung Galaxy M13 real-phone smoke test

**Date:** 2026-11-16 (local phone time, IST).
**Scope:** Controlled smoke checks on a disposable test note, **not full NT-001–079 acceptance or REL-16 signoff**.
**Status:** Smoke tests completed; further device interaction stopped when the phone switched to an active Samsung call screen.

## Build and device identity

| Field | Observed value |
|---|---|
| GitHub Actions run | [#43857426156](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43857426156), both CI jobs success |
| Source SHA | `b299cbb8972267eebe103493fa093f7e5cfa6f70` (`qa/b99-universal-apk-compile-fix-20261116`) |
| Artifact | `NotesTakingApp-debug-all-abis-200` (GitHub Actions ZIP, ID `14223070837`) |
| Exact `app-debug.apk` size | 61,560,611 bytes |
| Extracted artifact APK SHA-256 | `25eaf748dbe1c2039c66aa383f7ecc42bd9de2aeb3043dbc47f24fcb3b248025` |
| Installed base APK SHA-256 | **Exactly the same hash** from `adb shell sha256sum` of the installed `pm path` (one APK split); this establishes that the phone runs the exact B99 bytes |
| Package/version | `com.notestaking.app.debug`, `0.1.0-debug`, `versionCode=1` |
| Device | Samsung Galaxy M13, model `SM-M135F`, Android 14/API34, 1080×2408px, density 450, `armeabi-v7a` 32-bit CPU ABI |
| Test data | Single disposable note named `QA_GalaxyM13_20261116`; no unrelated notes were opened or edited |

## Observed results

| Check | Result | Evidence and limitations |
|---|---|---|
| ADB and app installation | **PASS** | Authorized device; installed debug package and exact APK hash matched B99 GitHub artifact |
| Open app / foreground | **PASS** | `MainActivity` foreground, app process running; Home library visible |
| Initial library / create affordances | **PASS** | Empty-library text, New Note, New Canvas, Import PDF, search and sidebar controls exposed |
| Create A4 Standard Note | **PASS** | New-note screen displayed A4 portrait; created and opened uniquely named disposable note |
| Draw with finger simulation | **PASS** | ADB swipe visibly produced green diagonal ink; a later swipe added a separate horizontal line |
| Autosave + Home + reopen | **PASS** | Saved lines visibly survived an explicit Home → library → tap note cycle; `notestaking.save` log records `true` and successful read |
| Insert page at end | **PASS (smoke)** | Add Page menu → Insert Page At End rendered second page; subsequent cold reopen screenshot showed two pages |
| Cold app restart and reopen | **PASS (smoke)** | Saved QA note, `am force-stop`, then `am start -W`: `LaunchState: COLD`, `Status: ok`, `TotalTime: 5389ms`; library showed QA note; editor reopened with both ink and second page |
| Area eraser | **PASS (smoke)** | Area eraser crossed a synthetic horizontal stroke and visibly removed a middle section, leaving both ends |
| Undo / Redo after canvas focus | **PASS (smoke)** | ADB Ctrl+Z restored erased section; Ctrl+Shift+Z re-erased it. Ink-pixel counts in same region: before 570 → eraser 467 → Undo 570 → Redo 467 |
| Unfocused keyboard shortcut | **INCONCLUSIVE** | Earlier synthetic Ctrl+Z/Ctrl+Shift+Z before a canvas touch did not change ink. Keys worked after eraser touch gave canvas focus; needs real keyboard / focus traversal testing under REL-12 |
| App crash | **No crash observed in tested flows** | Process remained alive through editor interactions. Crash-buffer query showed no `Process: com.notestaking.app.debug` crash; unrelated third-party app crashes were not counted |

**Autosave diagnostics:** `notestaking.save` reported successful persistent writes (`true`) for this QA note and successful reads after reopening and process restart. Example: `save 100ms ... 1275 bytes, true`; cold-reopen `open read 107ms ... 1275 bytes`. Different bytes after edits represent normal changing note content.

**Important precision:** This checked some portions of the intent behind NT-011, NT-012, NT-031, NT-039, NT-054 and NT-063 only. It **does not** meet every acceptance condition of those complete NT cases (several require tablets, fault injection, multiple formats, or hardware). Do not mark the original task cards `verified` or `device_verified` from this smoke report.

## Still not tested / not proven

- 200-page scanned/vector PDF memory/jank and Perfetto traces (REL-08).
- Real pressure/tilt/hover/palm and pen-button behavior (REL-09 requires an actual S Pen tablet; ADB finger swipes do not simulate stylus physics).
- Every eraser variant, dense ink, 3-crossing undo branch and split-pane isolation (REL-10).
- Zoom 25–400%, PDF crop/rotation 0/90/180/270 and split layout anchors (REL-11).
- Actual TalkBack spoken announcements, physical keyboard, 1.3×/2.0× font and narrow split panes (REL-12).
- Airplane mode, SAF denial/revoke, low storage, backing store failures, localizations, import/export and backup integrity (REL-13).
- New Quick Scratch UI and NP-102 handwriting recognition are **not in B99**.
- Signed production package/signature, installed upgrade path and final release approval (REL-14/16/19/20).
- One cold-start sample (5.39s) is **not a performance benchmark**; require repeated traces and control conditions.

## Safety and continuation

Only a disposable QA note was created and edited. No app install/uninstall, production note deletion, account change, network toggle, filesystem clear, permissions revocation or local Windows Gradle build was executed by the agent. The QA note was deliberately left on the device for review; do not delete it as part of a broad cleanup. Screens were visually inspected during the test; this report intentionally avoids archiving photos that may include personal phone status details.

**Next:** Continue non-destructive QA when the phone is available, capture the exact failed/passed case evidence and preserve the same APK hash across all measurements. Stop device interactions while Samsung's call UI is in the foreground. The full 79-case plan remains at `docs/github/UNIVERSAL_APK_PHONE_QA_2026-11-16.md`.
