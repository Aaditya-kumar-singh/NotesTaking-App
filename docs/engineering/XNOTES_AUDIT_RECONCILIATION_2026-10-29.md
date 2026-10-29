# XNotes historical problem-register reconciliation — 2026-10-29

**Source:** local `docs/audit/xnotes-problems-master.xlsx`, worksheet **Problems**: 45 original findings (XN-001 through XN-045).
**Integrated code baseline:** `main` commit `5a93bd7`.
**Important:** Historical Excel values still read **Open**. No row was overwritten or automatically closed. This is a **code integration/status triage**, not proof of real-device remediation.

## Evidence-backed implementation candidates already in main

These IDs have corresponding integrated source changes plus dedicated regression-test paths documented in `docs/engineering/`. **Status for all: Implemented candidate — pending original-issue reproduction and device acceptance**, not Closed.

| Findings | Integrated work | Evidence |
|---|---|---|
| XN-001, XN-007, XN-015, XN-024, XN-025 | Safe Save As / guarded navigation / background encoding | `SAVE_GUARD_NAVIGATION_2026-10-28.md` |
| XN-002, XN-003, XN-017, XN-026 | ZIP/XML limits, template extraction and failure cleanup | `IMPORT_ARCHIVE_XML_SAFETY_2026-10-28.md` |
| XN-004, XN-010, XN-022, XN-023 | Atomic session snapshot replacement, unique stage names, acknowledged results | `SESSION_PERSISTENCE_SAFETY_2026-10-28.md` |
| XN-005, XN-012, XN-030, XN-031 | DocumentsProvider canonical path, root and subtree guards; bounded copy | `DOCUMENTS_PROVIDER_STORAGE_SAFETY_2026-10-28.md` |
| XN-008, XN-018 | Bounded URI and clipboard imports | `BOUNDED_IMAGE_CLIPBOARD_IMPORTS_2026-10-28.md` |
| XN-032 | Explicit private-data Android backup policy | `PRIVATE_NOTE_BACKUP_POLICY_2026-10-28.md` |
| XN-037, XN-038 | Split-pane recovery and guarded navigation | `SPLIT_SESSION_PROCESS_DEATH_RECOVERY_2026-10-28.md` |

**Total evidence-backed candidate IDs:** 22. Branch ancestry for the underlying integrated safety branches was checked against `main`. These patches still need fault-injection/device regression evidence; treat them as **candidates**, not 22 resolved defects.

## Fix branches outside the integrated baseline

| Findings | Branch / evidence | Next action |
|---|---|---|
| XN-014, XN-016 | `fix/bounded-font-theme-imports` and import hardening | Compare exact paths to main, tests, and missing related call sites |
| XN-033, XN-034 | `fix/stylus-main-integration` | Integrate after review, then real palm/stylus tablet QA |
| XN-035 | `fix/precise-area-eraser-clipping` | Reconcile divergent branch, verify fast sweep and ink fragments |
| XN-036 | `fix/undo-redo-history-failure-safety` | Reconcile divergent branch, rerun exception-injection tests |

**Total branch-tracked IDs:** 6. Do not conflate branch CI success with a merged fix.

## No accepted closure evidence yet (or only partial related work)

The following 17 findings require dedicated investigation and verification, even if later implementation work overlaps them:

- **Performance/lifecycle:** XN-006 (onPause serialization), XN-020 (SVG crop read budget), XN-044 (GL teardown).
- **Provider coverage / compatibility:** XN-009 (Android provider negative instrumentation), XN-011 (DOCX import), XN-039 (split divider persistence).
- **Clipboard/export reliability:** XN-013 (share cache lifetime), XN-019 (clipboard file replacement), XN-021 (PNG compression failure).
- **Import reliability:** XN-027 (successful import temp cleanup), XN-028 (long import cancellation), XN-029 (PDF source staging limits), XN-043 (template text padding parse).
- **Native/rendering:** XN-040 (PDF page array allocation), XN-041 (Tree-sitter highlight output limits), XN-042 (failed GL bitmap decode retry).
- **Trash/feedback:** XN-045 (partial permanent deletion notice).

**Total not-yet-accepted IDs:** 17.

The groups above are mutually exclusive and cover all 45 IDs (22 + 6 + 17). They are *triage categories*, **not an assertion that 28 fixes are completed**. CI validation, targeted edge-case tests, source reinspection and hardware QA are separate gates. The original Excel workbook remains authoritative for historic finding descriptions and status until each row is individually reverified.

## Closure criteria

1. Find the original path and test reproducing the finding, and identify the integrating SHA on `main`.
2. Run targeted regression and the entire Android unit test/lint/debug build. Verify no skipped failures.
3. For platform/SAF/lifecycle/renderer issues, run an Android API and tablet device acceptance matrix with malformed input, low storage, permission denial, rotation and crash/reopen as applicable.
4. Update the Excel row with evidence, test result and final status **only after** completing the applicable gates.
5. Treat additional new defects independently rather than counting them under an unrelated original ID.
