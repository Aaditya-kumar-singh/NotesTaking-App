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

## NP-029 — One-tap PDF Read/Pan versus Annotate (Batch F, P1)

- Session `C-20261115-1120-gpt6`; parent local commit `bfba01411a141d3c051388db2522b65d65d35aea` (NP-028 remains in review). Atomically claimed `NP-029`, preserving all earlier reviewed commits and the shared A/B lane reservations.
- Read-only audited `ui/Editor.kt` (`selectTool`, `setPdfReadingMode`, tool-change callback and PDF source state), `ui/CanvasQuickAccess.kt` plus the PDF favorites/overflow wiring, `canvas/InteractionController.kt` PAN entry/tap/link behavior, `canvas/FreePointer.kt`, `core/tools/{PdfReadingToolChoice,Tool}.kt`, `docs/engineering/PDF_READING_ANNOTATION_UX_B08_2026-11-10.md`, and NT-026 phone/tablet acceptance. The one-tap Read/Annotate action already exists: Read arms PAN; Annotate restores the latest PDF editing tool through the non-serialized per-pane choice; PAN/SCREENSHOT/IMAGE do not replace it. It uses `setTool` without resetting page or viewport.
- No production behavior changed because there is no demonstrated missing feature or bug in the in-scope policy, and `ui/Editor.kt` / toolbar wiring is another agent's ownership. Added four focused cases to `core/tools/PdfReadingToolChoiceTest.kt`: exact NT-026 Read → Annotate → Eraser → Read → Annotate (plus Highlighter); every supported editing tool restored; transient tools and repeated Read ignored; independent split-pane tool memory.
- Source-only A–Z integrity, source/privacy, ownership and whitespace checks required before local commit. Kotlin/JUnit and Android builds are prohibited locally; GitHub exact-SHA CI, viewport/page preservation, mode button UI, and NT-026 phone/tablet gestures remain **unverified** and must not be marked complete. No push, rebase, manual checklist ticks or non-owned files modified.

## NP-030 — pen, dashed, calligraphy, taper and speed brushes (Batch F, P1)

- Session `C-20261115-1120-gpt6`, parent local commit `f570af43c31a72b7ac3cdf7d429517fee6c75a8f` (NP-029 is in review). Continued the active `NP-030` claim after the preceding Chat C turn was interrupted; no duplicate claim or session replacement.
- Inspected shared `core/tools/{ToolConfig,Tool}.kt` defaults and persistence, `core/stroke/{StrokeEngine,WetRibbon,StrokeGeometry}.kt` pressure/speed/direction/taper geometry, `core/model/{Stroke,DrawStyle}.kt` live and finished rendering/config snapshots, `core/infinite/{ItemMesher,StrokeTessellator}.kt` paged/infinite brush signatures, read-only paged `canvas/InteractionController.kt` pen-down settings, and existing unit tests. All five brushes already have independent implementations; no demonstrated production bug warranted a risky geometry change.
- Added `core/model/BrushToolsTest.kt`: five distinct pen signatures (pressure, dashed polyline, calligraphy direction, taper tip, speed thinning), 20 strokes per brush across 25%/100%/400% zoom ensuring finite geometry and preserved snapshots, plus restyle/restore retaining speed, taper, direction and dash parameters without affecting another stroke.
- A-Z catalog integrity, Python source validation (0 problems), private-backup guard, exclusive C file ownership and whitespace checks passed. Source-only validation cannot establish Kotlin test pass, real brush quality, persistence after restart, or device NT-027: these remain for exact-SHA GitHub Android CI and physical S Pen phone/tablet acceptance. No push, Gradle, APK build or release performed.

## NP-031 — highlighter transparency and inverse blend (Batch F, P1)

- Session `C-20261115-1120-gpt6`; parent local commit `993230120e11b19fe8552647b7aad218aa3c5211` (NP-030 in coordinator review). Claimed NP-031 independently, preserving all existing reviewed SHAs.
- Inspected `core/model/Stroke.kt` opacity and blend dispatch, `core/infinite/ItemMesher.kt` MULTIPLY/SCREEN passes, read-only `gl/CanvasScene.kt` masked compositing, `canvas/{CanvasView,CanvasState}.kt` raster-backed highlighted live overlays, `core/pal/Renderer.kt`, `core/tools/ToolConfig.kt` highlighter intensity defaults and read-only settings/codecs, NT-027 and NP-031 acceptance.
- Found a concrete 100%-intensity mismatch in shared Stroke.paint: `renderColor.a == 255` took the direct opaque-paint branch instead of the configured MULTIPLY or inverse SCREEN layer. This could affect paged overlays and export paths even though CanvasView highlighter-cache overlays and infinite mesh already choose blend correctly. Changed the opaque branch to exclude `Tool.HIGHLIGHTER`; the existing layer branch now handles the full intensity range without changing non-highlighter pens or persisted formats.
- Added `core/model/HighlighterBlendTest.kt` source regression for normal/inverse ink at 10%, 35%, 90% and 100% alpha, verifying one blended layer with resolved alpha plus the matching infinite canvas pass, and that an ordinary opaque pen still paints without a blend layer.
- A–Z catalog integrity, Python source (0 problems), backup privacy, exclusive C ownership and Git whitespace checks passed. Kotlin/JUnit, exact-SHA GitHub CI, real dark/light/PDF background display, 20 highlighter strokes, transparency and reopen/export checks are **not yet executed**; physical S Pen/phone NT-027 verification required. No push, merge or Android build.

## NP-041 — selection/lasso-based erasing semantics (Batch G, P1)

- Session `C-20261115-1120-gpt6`; parent local commit `cbdeb6058a2f2703b602d5fb81c456d7f9db429b` (NP-031 remains in coordinator review). Confirmed same registered session, clean Agent C worktree and exclusive ownership; atomically claimed `NP-041`.
- Inspected shared `canvas/SelectionMath.kt` lasso/band hit tests and locked-item exclusion, paged `canvas/InteractionController.kt` selection/lasso/delete/cache repair, `core/history/{Command,History}.kt` undo/redo and touched regions, `core/model/Groups.kt` group expansion; read-only `ui/InfiniteEditor.kt` plus `core/infinite/CanvasCommands.kt` exact-slot undo. NT-033 requires two-object lasso delete and single Undo restoring only those objects.
- Found paged `deleteSelection()` used `EraseItems`, whose legacy undo appends removed items to each page, changing z-order after undo. Infinite canvas already captures original z-slots. Added `EraseItems.capture(removals)` to record only selected item references and original per-page indices before removal, restoring slots in ascending order (no full-page snapshots). Paged selection deletion now uses this path. Other historical eraser uses of `EraseItems` retain their existing behavior; no persistent data/schema edits.
- Added `core/history/HistoryTest.kt` regression for mixed z-order, unsorted selections across pages, idempotent Undo and Redo. Added `canvas/SelectionCacheRepairTest.kt` end-to-end model test for lasso membership, a locked object inside the loop, two selected objects amid unselected layers, exactly one Undo restoring original ordering and Redo removing only the same members. Existing PDF/background cache preservation tests remain intact.
- Permitted A–Z integrity, Python source checks, privacy validation, C-only file ownership and `git diff --check` required before commit. Kotlin/JUnit, exact-SHA GitHub CI and real NT-033 stylus lasso/delete/undo across paged/PDF/infinite surfaces remain **unverified**; no APK build, push, merge, or checklist tick by worker.

## NP-042 — eraser size slider and cursor ring (Batch G, P1)

- Session `C-20261115-1120-gpt6`, parent local commit `f2fbcf2ec216a04647408a41dc4750b93e4918b3` (NP-041 still in review). Confirmed clean Agent C worktree, current developer ref `ab0774ca9fc6503f41aab238cdda9399cd0d3425`, exclusive C ownership and atomically claimed NP-042.
- Inspected paged `canvas/InteractionController.kt` eraser hit-test, sweep, hover and overlay ring, `core/geometry/EraserSweep.kt`, `core/model/Stroke.erasedBy` nib-width-aware boundaries, `core/tools/{ToolConfig,ToolConversions}.kt` persisted slider radius 1–80 (default 24), read-only `ui/Popups.kt`, `ui/Editor.kt`, `ui/InfiniteInteraction.kt`, `ui/InfiniteEditor.kt`, `gl/InfiniteCanvasView.kt`, and NT-034 S Pen zoom/size requirements.
- Corrected a paged-canvas stale hover preview: the slider calls `InteractionController.setToolConfig` but changing the configuration previously did not request a render. Config changes for the eraser now request a frame, avoiding a stationary cursor displaying the previous size. Centralized paged eraser's viewport radius in `eraserCursorRadiusPx()`, used by both sweep interpolation and drawn ring; the actual erase still uses `eraserRadius()` in content units. Existing scale on/off semantics and stored settings are unchanged.
- Added `canvas/EraserCursorSizeTest.kt` for sizes 4/24/80, zoom 25/100/400%, scale enabled/disabled, matching ring and erase radii, two-point stroke cut boundaries including nib thickness; also immediate config-change redraw, deduplicated updates and unchanged unrelated pen settings.
- An infinite-canvas stationary hover refresh when the slider changes is potentially dependent on `ui/InfiniteEditor.updateToolConfig` and the cached GL cursor radius in `gl/InfiniteCanvasView`. These paths are outside Agent C's ownership; coordinator must review/allocate if a real device reproduction shows a stale ring. Neither source file was modified.
- A–Z integrity, Python source validator (0 problems), privacy checker, Chat C file-ownership guard and `git diff --check` passed. Kotlin/JUnit, exact-SHA GitHub Android CI and real S Pen NT-034 cursor/erased-width tests are unverified. No APK build, branch reset, integration push, or checklist tick.

## NP-048 — one erase sweep is one undo transaction (Batch G, P1)

- Session `C-20261115-1120-gpt6`; parent local commit `0599305f1f6edbfd24d1b2dc638cbfc88260ea94` (NP-042 in review). Atomically claimed NP-048. LocalMCP became unavailable mid-edit in the previous conversation, then recovered; confirmed the existing active claim and uncommitted C-only files before continuing, without a second claim, session replacement, branch reset or lost reviewed SHA.
- Audited paged `canvas/InteractionController.kt` eraser stroke/area loops, interpolation, cancelled touch/hover/pinch/switch-tool paths, `core/history/{History,Command}.kt` composite transaction and branch clearing, `core/model/Stroke.erasedBy`, `core/geometry/EraserSweep`, read-only infinite editor `commitErase`, and `core/infinite/{EraseSession,CanvasCommands}.kt`. Both editors already group the entire drag into one history push; area mode stores the net before/after page state, infinite mode coalesces repeated cuts by origin.
- Fixed two transaction edge cases in the paged controller: the erase area/stroke mode is now latched at pen-down so a config change cannot make pen-up commit the wrong representation; whole-stroke removals now record each item's current per-page z-slot at removal instead of appending the restored item on Undo. `EraseItems.fromSweep` restores these reverse-order removal slots on Undo; `EraseItems.capture` (selection snapshot from NP-041) retains its separate ascending original-slot restoration. Uses identity for removed object lookup, and keeps existing format and non-erase History semantics.
- Added `canvas/EraseSweepTransactionTest.kt`: cross one stroke three times in one area sweep, expect four independently editable fragments, one Undo restoring the exact initial list, Redo restoring exact fragments, and a new edit clearing redo; test mid-sweep eraser mode toggles in both directions and cancel via tool switch. Added `core/history/HistoryTest.kt` test for whole-stroke removal order across multiple pages, one Undo and repeated Redo/Undo. Added `core/infinite/EraseSessionTest.kt` for three crossings as one History command and redo-branch invalidation.
- Permitted A–Z task integrity, Python source checks, backup privacy, C ownership and whitespace checks passed. Kotlin/JUnit/Android execution is prohibited locally and **has not been run**. Exact-SHA GitHub CI and real S Pen NT-039 (multi-crossing, cancelled gestures, undo/redo and new edit) are pending; do not mark checked or device verified. No worker push, merge or APK build.

## Handoff template

- Task ID / batch / parent SHA:
- Session ID / claim time / allowed changed files:
- Actual callers and source inspected:
- Source + privacy + ownership + whitespace checks:
- Tests added (cloud execution not claimed):
- Local commit SHA:
- Known risks, dependencies, pending hardware acceptance:
- Coordinator review outcome / verified GitHub run:
