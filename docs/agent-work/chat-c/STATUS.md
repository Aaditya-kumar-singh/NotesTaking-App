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

## NP-040 — whole-stroke eraser removes only intersected lines (Batch B, P0)

- Session `C-20261115-1120-gpt6`; parent local commit `b1a082182cfb90ff389bc6036a9c4edc9feaea28` (NP-039 handed off in review). Claimed independently while Batch A review and CI are outstanding.
- Inspected both paged `canvas/InteractionController.kt` and infinite `core/infinite/EraseSession.kt` erase paths, `core/model/{Stroke,EraseRule}.kt`, spatial index, existing whole/area tests, settings mode persistence and NT-032. Existing `EraseMode.STROKE` behavior already removes the full intersected item rather than partial fragments.
- Hardened both permitted erase entry points to reject non-finite coordinates/radii and nonpositive eraser radii, preventing malformed inputs from reaching page/document removal or infinite-canvas spatial index queries. Did not change persisted settings, the separate area mode, or other lanes' source files.
- Added `core/infinite/EraseSessionTest.kt` regressions for a long sparse hit stroke with an adjacent untouched line and a locked line, exact one-command Undo/Redo order, and malformed eraser input under both modes.
- Source-only checks, privacy/ownership/whitespace and A-Z integrity required before local commit. Android CI exact SHA, Kotlin execution, and real stylus stroke/sweep tests (NT-032) remain pending and are not to be marked passed.

## NP-047 — dense-ink area eraser flicker (Batch B, P0)

- Session `C-20261115-1120-gpt6`; parent local commit `5cc3fb71b5eb4bbf02dc4b824779f1b66ba461b6` (NP-040 handed off in review). Claimed next independent P0 under updated nonblocking three-chat protocol.
- Reviewed paged area eraser `InteractionController.eraseAt/eraseAreaFromPage`, `CanvasState` async ink/sharp caches, `CanvasView` blitting, shared `EraserSweep`, `UndoCacheRepairTest`, `PlaybackFadeCacheTest`, and 30-page S Pen NT-038 requirement. Existing B15 bounded repaint retained; no Android renderer or other lane files modified.
- Source fix: while a previously uncached page has its initial `scheduleInk` job in flight, `repairRegion` now queues the dirty area for replay at the job's existing guarded publish rather than returning false and forcing `invalidatePage` to discard the in-flight build. Edits still replay against the current page items and cannot revive erased strokes.
- Added `canvas/UndoCacheRepairTest.kt` source regression with a held async job: erase during cold build, assert no redundant build, publish once, replay the clear without painting the removed dot, and retain the resulting cache. This addresses a definite blank-frame latency path, not a claim that all dense-PDF flicker is fixed.
- Source, A-Z, privacy, ownership, whitespace checks required. GitHub exact-SHA Kotlin/Android CI and real 30-page dense PDF erasing with stylus (NT-038) are still unverified; do not mark the batch or device gate complete.

## NP-025 — live ink smoothing and pen-up commit (Batch E, P1)

- Session `C-20261115-1120-gpt6`; parent local commit `f6ac847acd00345765dc6bae7e1a8bc4e8495991` (NP-047 remains in review). Claimed independently with the current three-chat protocol; no other agent's files or reviewed SHAs changed.
- Reviewed paged `canvas/InteractionController.kt` live/finished stroke routing and stroke simplification, `canvas/FrontInk.kt` pending front-buffer takeover and cache handoff, `canvas/CanvasView.kt` frame commit callbacks, `canvas/WetInkCache.kt`, `core/model/Stroke.kt`, plus infinite-canvas calls (read only) and existing wet ribbon/GL pad tests. The wet ribbon already preserves smooth ink while drawing and changes to finished geometry at pen-up.
- Fixed a delayed front-buffer takeover: a queued frame callback may run after the originating stroke was finished, abandoned, or replaced. Activation now requires the same still-live stroke on both direct and stand-down paths. A genuinely live stroke that activates after a frame delay immediately presents its latest ribbon even when no later MOVE event arrives, avoiding a blank/reappearing nib.
- Added `canvas/FrontInkHandoffTest.kt` source coverage for abandoned, replaced, finished, and still-active takeover ownership. Added `canvas/WetInkCacheTest.kt` source coverage that committed strokes do not also reuse a stale live raster (single owner/paint path after pen-up). Existing taper, speed, smoothing, PDF/page-crossing behavior and native note codecs untouched.
- Source, A-Z, backup privacy, C ownership and `git diff --check` checks required before local commit. Kotlin/JUnit and Android tests are forbidden locally; GitHub exact-SHA CI plus physical S Pen frame timing, cancellations, rapid consecutive strokes and no-duplicate-ink acceptance remain unverified.

## NP-026 — stylus-only ink versus finger drawing (Batch E, P1)

- Session `C-20261115-1120-gpt6`; parent local commit `70f1c152ae712d9c22d690f58db598da2f8e5bbe` (NP-025 in coordinator review). Claimed independently and retained all previous review SHAs intact.
- Inspected UI preference wiring (read-only `settings/Preferences.kt`, `ui/Editor.kt`, `ui/InfiniteEditor.kt`, `ui/InfiniteInteraction.kt`, quick access and preferences panel), paged `canvas/InteractionController.kt`, shared `canvas/FreePointer.kt`, tool metadata, stylus-button latch, mixed-pointer routing, existing tests and NT-025 S Pen acceptance. Existing B14/B16 behavior already persists the preference and applies it to both editors; finger-only pans when disabled, stylus/eraser remains armed, and pen pressure is independent of finger drawing.
- No speculative runtime change to already-implemented input routing. Added `canvas/FreePointerTest.kt` regression table for all 13 ink/eraser/gesture tools in both toggle states, and for navigation/typing/placement tools unaffected by the preference. This catches regressions if new drawing tools accidentally bypass the stylus-only gate.
- Only Chat C-owned test and status paths changed. Pending gates: Kotlin/JUnit GitHub exact-SHA CI, physical S Pen/phone NT-025 (finger pan vs ink, two-finger zoom, stylus tip and button eraser), and UI preference persistence across relaunch. Do not claim device verification or acceptance from source tests.

## NP-027 — two/three-finger gestures and pinch zoom (Batch F, P1)

- Session `C-20261115-1120-gpt6`; parent local commit `64654d2a77f99da8dd0f18ea7b3219f83232bf6d` (NP-026 remains in coordinator review). Claimed atomically without touching earlier reviewed SHAs.
- Inspected `canvas/{CanvasView,MultiFingerTap,InteractionController,CanvasState,PenContactRouter}.kt` and read-only `gl/InfiniteCanvasView.kt`, `ui/InfiniteInteraction.kt`, editor gesture preference bindings and existing zoom/gesture tests. Both surfaces use the shared `MultiFingerTap` detector; paged/infinite pinch handlers already anchor zoom to the initial finger midpoint in content space.
- Fixed a definite false-action path: movement was only checked on ACTION_MOVE, so a quick pinch/swipe reported at POINTER_DOWN, POINTER_UP, or final UP could fire a two/three-finger tap and cancel the pinch. Now transitions and terminal coordinates are checked, short significant span changes are rejected, cancelled lifts are rejected, and the detector resets after terminal UP/CANCEL. A replacement third finger cannot be counted as a clean two-finger tap. The Android MotionEvent adapter is reused across MOVE frames.
- Added `canvas/MultiFingerTapTest.kt` pure JVM source tests for clean two/three-finger action once, movement at pointer transitions and final lift without MOVE, short pinch-span drift, replacement pointer IDs, stylus/mouse mixed gestures, cancelled lifts, long press, and four-finger noninterference. No other lane source or preferences/format files changed.
- Permitted A-Z catalog, source, privacy, C-ownership and whitespace checks must pass before committing. Kotlin/JUnit/Android execution, exact-SHA GitHub CI, and physical phone/tablet multi-finger tap/pinch anchor/no-phantom-ink acceptance remain unverified. No local build or push.

## NP-028 — stylus side-button and rear-eraser mappings (Batch F, P1)

- Session `C-20261115-1120-gpt6`; parent local commit `4d627d6b8c2a22b592a16f208c2d8245b258ff44` (NP-027 remains in review). Claimed atomically; previous reviewed SHAs preserved.
- Inspected shared `canvas/StylusButtons.kt` and matching `StylusButtonLatchTest.kt` / `ButtonClickTest.kt`, paged `canvas/InteractionController.kt`, read-only infinite editor/controller and `ui/Editor.kt` keycode mapping, preferences persistence, `MainActivity.kt` focus/touch forwarding, and NT-025 tablet acceptance. Both canvases already give `TOOL_TYPE_ERASER` precedence over configured side-button tools and restore the armed tool on new tip contact without changing persisted tool state.
- Fixed shared key hold latch: earlier a single `keyHeld` boolean let a different stylus button's key-up clear an active button. A shared per-key pressed-code set now retains the effective hold until all keys release; focus loss clears the set and cancelled clicks. All key-repeat events are idempotent.
- Fixed shared button click-versus-hold policy to treat `TOOL_TYPE_ERASER` as pen contact, so reversing the pen while a button is pressed cannot fire a tap-only gesture. Added pure tool-type overload for `heldFor` to test rear eraser/finger/mouse isolation.
- Added regression coverage for overlapping stylus keys across panes, unpaired key-up, focus loss, rear-eraser precedence, and rear-tip click suppression. No edits to shared UI, platform, settings, source documents or other lanes.
- A–Z integrity, source Python (0 problems), privacy, C ownership and whitespace checks passed before commit. Kotlin/JUnit/Android execution, exact-SHA GitHub CI, and real S Pen tablet NT-025 button/eraser/hover/auto-repeat/focus-restore acceptance remain pending. No push or APK build.

## Handoff template

- Task ID / batch / parent SHA:
- Session ID / claim time / allowed changed files:
- Actual callers and source inspected:
- Source + privacy + ownership + whitespace checks:
- Tests added (cloud execution not claimed):
- Local commit SHA:
- Known risks, dependencies, pending hardware acceptance:
- Coordinator review outcome / verified GitHub run:
