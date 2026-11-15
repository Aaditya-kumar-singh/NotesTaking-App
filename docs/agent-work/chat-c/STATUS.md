# Chat C - core/canvas reliability ownership and handoff

**Provisioned by:** Three-GPT A-Z coordinator setup, 2026-11-15.
**Branch:** `agent/chat-c-core-canvas-reliability`.
**Worktree:** `.agent-worktrees/chat-c`.
**Pinned base:** `refs/heads/agent/chat-c-start-20261115`.
**Allowed edits:** `app/src/main/java/com/notestaking/app/core/**`, `app/src/main/java/com/notestaking/app/canvas/**`, matching `app/src/test/java/com/notestaking/app/{core,canvas}/**` and this `docs/agent-work/chat-c/**` directory. Shared UI, strings, CI, platform/format, A/B and Gemini paths are out of scope.

Read [the current three-chat protocol](../../github/THREE_GPT_APK_WORKFLOW.md), [the full A-Z list](../../github/APK_BATCH_A_TO_Z.md), and `AGENTS.md` before editing. Check `triad.py status`, register a unique initial C session using the administrative coordinator's `replace --slot C` after the worktree is clean, then atomically claim a C task. Never claim A/B/Gemini ownership or publish from this checkout.

## Pending work

2026-11-15: Session `C-20261115-1120-gpt6` registered and claimed `NP-020` (Batch A, P0). Source review found pen-first touches were not tracked by `PenContactRouter`, allowing later resting fingers to enter the controller as extra pointers. The bounded fix records the initial pen pointer and down time so all subsequent mixed events are isolated to that pen, preserving pen-up completion. Android MotionEvent runtime and tablet stylus acceptance remain unverified; GitHub CI and physical-device QA are required before acceptance. All task state remains in the shared local Git SQLite ledger. CI and physical-device results must be recorded after they actually run.

## NP-021 — mixed pointer handoff (Batch A, P0)

- Session: `C-20261115-1120-gpt6`; parent local commit: `331fa8ee3d3950cfa367172dfa9fdb3e327f7ecb` (NP-020 in coordinator review).
- Reviewed both consumers of the shared router: paged `canvas/InteractionController.kt` and infinite `ui/InfiniteInteraction.kt`, the two/three-finger tap interceptor, active pen-lift handling, existing `PenContactTest.kt`, and historical palm-continuation notes. No files outside C ownership changed.
- Changes: when the active pen pointer unexpectedly disappears, synthesize `ACTION_CANCEL` instead of dropping its active ink state; route pointer actions through a unit-testable policy; omit historical MOVE samples timestamped before pen-down during finger-first takeover. Both canvases inherit the shared fix.
- Regression source: `canvas/PenContactTest.kt` covers mixed pointer down/up, finger-first continuation, pen termination and system cancellation. Kotlin unit test has not run locally (Windows Android build/test execution prohibited).
- Validation: Python source check, backup privacy check, exclusive C ownership check, and `git diff --check`; CI, MotionEvent replay, and physical pen/palm hardware acceptance remain pending.

## NP-022 — stylus/eraser bypasses page scrollbar (Batch A, P0)

- Session: `C-20261115-1120-gpt6`; parent local commit: `a292a5b5034549a77a08b8ba7765034095a87c70` (NP-021 in coordinator review).
- Inspected `canvas/CanvasView.kt` scrollbar interception, shared `PenContact` and `PenContactRouter`, `canvas/InteractionController.kt` consumer, `gl/InfiniteCanvasView.kt` (no page scrollbar), existing `PenContactTest.kt`, and B09 engineering notes. B09 had already implemented stylus/eraser bypass and finger-scrollbar-to-pen takeover; preserved both paths.
- Edge-case fix: track the exact finger pointer ID that began a scrollbar drag, rather than assuming index 0 remains the owner. When that finger lifts while another remains, ignore the remaining finger's motion instead of moving the scrollbar; the next stylus/eraser tip may still take over. Reset the tracked pointer on completion/cancellation and display active thumb styling only while its owner is present.
- Regression source: `canvas/PenContactTest.kt` checks initial scrollbar owner vs unrelated finger lift and cleared owner. Existing tests check stylus/eraser exclusion and finger-first takeover. Android/Kotlin execution prohibited locally; pending GitHub CI and S Pen tablet NT-019/020/021/022, vertical and horizontal bands, palm, mouse/finger, and data safety acceptance.

## NP-023 - pressure-sensitive pen strokes (Batch A, P0)

- Session: `C-20261115-1120-gpt6`; parent local commit: `e342a3de7ad7131bc1ddb9ae68dd7c69f1a25cc2` (NP-022 in coordinator review).
- Reviewed Android historical/current/down pressure reads in `canvas/InteractionController.kt` and the read-only `ui/InfiniteInteraction.kt`, `PenContactRouter`, `core/model/Stroke.kt`, `core/stroke/{StrokeEngine,WetRibbon}.kt`, tool defaults, and existing stroke geometry tests. The controllers already pass valid pressure through to the engine, including zoom-dependent smoothing. Only core-owned files changed.
- Fixed the shared new-sample ingestion boundary: bound stylus pressure to `[0,1]`, with a full-pressure fallback for non-finite hardware readings. The packed sample and growing wet ribbon now receive the same finite value for both canvas input pipelines; existing serialized samples and codecs are not rewritten.
- Added `core/model/LiveStrokeRibbonTest.kt` regression source for negative/over-range/NaN/infinite pressure and for light versus firm pressure width at 25%, 100%, and 400% zoom, before and after pen-up.
- Verification: `check_project.py`, `check_backup_privacy.py`, `check_lane.py` and `git diff --check` passed. Local Kotlin/Gradle execution prohibited by repo policy. GitHub exact-SHA CI and S Pen device pressure/width/last-frame checks (NT-023) remain required; source checks alone do not establish hardware pass.

## NP-039 — area eraser splitting and surviving segments (Batch A, P0)

- Session `C-20261115-1120-gpt6`; parent local commit `8eed0b62bd964189dbbde039f75e2df21fef96d3` (NP-023 remains in review). Task was already claimed in the shared triad ledger; resumed that active claim without another writer or branch mutation.
- Examined both consumers of `Stroke.erasedBy`: paged `canvas/InteractionController.kt` and infinite `core/infinite/EraseSession.kt`, raw/polyline geometry, zoom-scaled eraser radii, current `StrokeEraseTest` and `EraseSessionTest`, test NT-031. The established two-point segment clipping and interpolated pressure/time logic remain intact.
- Fixed a calligraphic edge case: the eraser hit test and area splitter now use a conservative bound on the largest nib half-width, including directional width variation, so visible broad nib ink cannot escape clipping when the eraser touches past the base-width radius. Plain pen behavior is unchanged.
- Added `core/model/StrokeEraseTest.kt` coverage for sparse two-point ink split at 25%, 100%, 400% draw zoom (two independently editable fragments, interpolated pressure and time, original untouched), plus a broad calligraphy-nib grazing case.
- Checks: A-Z integrity, Python source validator, backup privacy validator, Chat C file ownership and `git diff --check` required. Kotlin tests, exact-SHA CI, S Pen NT-031 and actual file save/reopen cannot run locally under Windows policy; all remain pending.

## Handoff template

- Task ID / batch / parent SHA:
- Session ID / claim time / allowed changed files:
- Actual callers and source inspected:
- Source + privacy + ownership + whitespace checks:
- Tests added (cloud execution not claimed):
- Local commit SHA:
- Known risks, dependencies, pending hardware acceptance:
- Coordinator review outcome / verified GitHub run:
