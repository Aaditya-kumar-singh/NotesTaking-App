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

## Handoff template

- Task ID / batch / parent SHA:
- Session ID / claim time / allowed changed files:
- Actual callers and source inspected:
- Source + privacy + ownership + whitespace checks:
- Tests added (cloud execution not claimed):
- Local commit SHA:
- Known risks, dependencies, pending hardware acceptance:
- Coordinator review outcome / verified GitHub run:
