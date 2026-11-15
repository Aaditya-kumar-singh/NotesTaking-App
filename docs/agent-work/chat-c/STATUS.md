# Chat C - core/canvas reliability ownership and handoff

**Provisioned by:** Three-GPT A-Z coordinator setup, 2026-11-15.
**Branch:** `agent/chat-c-core-canvas-reliability`.
**Worktree:** `.agent-worktrees/chat-c`.
**Pinned base:** `refs/heads/agent/chat-c-start-20261115`.
**Allowed edits:** `app/src/main/java/com/notestaking/app/core/**`, `app/src/main/java/com/notestaking/app/canvas/**`, matching `app/src/test/java/com/notestaking/app/{core,canvas}/**` and this `docs/agent-work/chat-c/**` directory. Shared UI, strings, CI, platform/format, A/B and Gemini paths are out of scope.

Read [the current three-chat protocol](../../github/THREE_GPT_APK_WORKFLOW.md), [the full A-Z list](../../github/APK_BATCH_A_TO_Z.md), and `AGENTS.md` before editing. Check `triad.py status`, register a unique initial C session using the administrative coordinator's `replace --slot C` after the worktree is clean, then atomically claim a C task. Never claim A/B/Gemini ownership or publish from this checkout.

## Pending work

No task has been claimed or implemented by Chat C at initial provisioning. All task state is in the local common Git SQLite ledger, not this file. CI and physical-device results must be recorded after they actually run.

## Handoff template

- Task ID / batch / parent SHA:
- Session ID / claim time / allowed changed files:
- Actual callers and source inspected:
- Source + privacy + ownership + whitespace checks:
- Tests added (cloud execution not claimed):
- Local commit SHA:
- Known risks, dependencies, pending hardware acceptance:
- Coordinator review outcome / verified GitHub run:
