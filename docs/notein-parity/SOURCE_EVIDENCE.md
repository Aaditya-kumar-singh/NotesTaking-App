# Source evidence: exact Notein APK and comparison limits

## Artifact identity

- File: `C:\Users\WHITE-SNAKE\Desktop\xnotes-android\Notein_Official.apk`.
- Exact identity rechecked 2026-11-11: `com.orion.notein.global`, v1.2.607.0 / code 346; SHA-256 `3a5b703f06174117741e3c4b9f27ecabefe7d02e9be65084c344e67a372353f6`.
- Historical read-only investigation output: `E:\Notein_ReverseEngineering\`.
- Original APK and Notein evidence remain outside the NotesTaking source commits; do not upload or embed them into the app repository.

| Evidence ID | Report in evidence folder | What it actually establishes |
|---|---|---|
| **N09** | `09_initial_findings.md` | Package/version/signature/resources, classes and summarized phase findings; static inventory |
| **N13** | `13_eraser_deep_dive.md` | Separate Point/Stroke/Selection eraser routes; point-erasure segmentation, masks, pressure, UI control declarations |
| **N18** | `18_pdf_workflow_deep_dive.md` | PDFium-backed PDF paper, layered page coordinates and page metadata; PDF export UI routes |
| **N21** | `21_canvas_stylus_gesture_deep_dive.md` | MotionEvent stylus/finger dispatch, OEM detector branches, scale/scroll, pressure and page offsets |
| **N26** | `26_undo_autosave_recovery_deep_dive.md` | Operation-based undo/redo, persistence coroutines, Room schema inventories, version/recovery UI; no verified crash durability |
| **N28** | `28_native_note_package_saveas_restore_deep_dive.md` | Native ZIP packaging, Save Page As new-note route, export paths and security concerns in reference app |
| **N29** | `29_ui_ux_static_inventory.md` | Six nav graphs, 664 general layouts, 26 tablet layout overrides; library/editor/backup/settings families, not all runtime tested |
| **N30** | `30_ui_ux_layout_accessibility_deep_dive.md` | Sampled icon/layout dimensions, tablet vs phone XML, candidates for touch/accessibility review, not actual hit-area proof |
| **N31** | `31_live_first_launch_ux_audit.md` | Phone onboarding, guest account prompt, consent modal and skip/friction findings |
| **N32** | `32_live_library_editor_audit.md` | Real phone library/Create/Standard Note/Infinite Canvas/template picker, one finger ink; Undo/Redo inconclusive |
| **N33** | `33_live_editor_export_page_toolbar_audit.md` | Real phone toolbar/overflow, export *dialog only*, page manager, toolbar visibility controls and label/hit-area candidates |

The SHA-256 above was validated again on the local APK before constructing this plan. REA/JADX method evidence was previously recorded in `E:\Notein_ReverseEngineering\*.json`, alongside static decoded resources. This pass uses those saved reports and fresh NotesTaking source inspection; it does **not** claim a new REA-engine run.

## Verified Notein behaviors, versus unverified claims

**Observed on phone:** Guest library, Create menu, Standard Note, Infinite Canvas option, page/cover template choices, initial editor, movable pen toolbox, one finger stroke, editor overflow, export format/options dialog, page manager and toolbar customization UI. The export was cancelled. No Notein output file was validated.

**Verified statically only:** Geometry-based pixel/whole-stroke eraser implementations, PDF storage and page-offset code, temporary ink and input routing, undo/redo actions, local persistence and Room schemas, native archive ZIP and Save Page As code. Static presence is not a claim of perfect runtime behavior.

**NOT established:** Notein live Point/Stroke eraser usability, S Pen palm rejection on a tablet, PDF export correctness or true editable annotations in another PDF reader, handwriting-to-text accuracy, OCR network/privacy behavior, cloud backup correctness, crash recovery and physical touch hit areas. No permission escalation, user-owned Notein document, native export or cloud account was used in the investigation.

## Independent product decisions

- Notein's four format cards (Standard PDF, Flattened PDF, image `JPGE` in APK copy, Native Format) are **reference behaviors**. NotesTaking currently has shareable visual PDFs, PNG page images and native `.xnote` saving; each should be labeled honestly. Do not expose format switches that the exporter cannot implement.
- Notein's legal/account/AI/cloud/subscription surfaces are **optional product research**, not requirements to add mandatory login. Offline-first notes are a hard constraint.
- Notein's reported ZipFile writer and one-shot FileChannel transfer included unresolved potential failure cases. Prefer NotesTaking's existing bounded ZIP reader, safe document tree, and atomic source-preserving writes; verify rather than reimplement from the reference.

## How each NP requirement is grounded

Each group in [FEATURE_MATRIX.md](FEATURE_MATRIX.md) lists its Nxx report evidence and relevant NotesTaking source files. These paths locate *where to audit or implement*, not a proof of all edge cases. The acceptance gate is in [TEST_PLAN.md](TEST_PLAN.md). When a future implementation is complete, record exact SHA, CI and device/test case IDs in [PROGRESS.md](PROGRESS.md).
