# Three ChatGPT contributors - A-Z NotesTaking APK execution protocol

**Project:** Aaditya-kumar-singh/NotesTaking-App
**Canonical integration branch:** `developer`. **No direct agent pushes to `main` or `master`.**
**Authoritative task list:** [APK_BATCH_A_TO_Z.md](APK_BATCH_A_TO_Z.md) and machine snapshot [APK_BATCH_TASKS.json](APK_BATCH_TASKS.json).
**Live ownership and progress:** `python tools/agents/triad.py status`, using the **common local Git SQLite database**. The existing Chat A/B/Gemini coordinator stays intact.

## Scope and definition of done

- **A-Z = 26 batches of five tasks (130 tasks):** 110 existing NP feature/QA cards plus 20 release readiness tasks. The 79 existing NT tests are mapped into the cards; see `docs/notein-parity/TEST_PLAN.md` for complete procedures. **All five entries stay visible even when a task is already CI-tested or explicitly deferred.**
- `SOURCE`, `PARTIAL`, `LOCAL`, `PUBLISHED`, and `GAP` are **snapshot evidence classifications**, not task-completed states. The live ledger starts at pending until reviewed.
- **Beta/debug APK is already buildable** (B67, exact SHA `75d89ba`, [CI #43679116911](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43679116911) passed). **Do not force 130 tasks before every debug build.** Public/signed-release gates require the selected P0/P1 scope, full-ABI CI, manual provider/stylus/device tests, and tag/signing/rollback review. P2/P3 can be deferred only with an explicit recorded scope decision; an incomplete required task cannot be silently waived.
- **Five tasks form one review/integration batch.** Any combination of A, B, C and coordinator/device evidence can complete them. The batch is locally review-ready only when all five are `local_ready`, `device_verified`, or explicitly `deferred`. Run `python tools/agents/triad.py batch-ready A`. A ready batch is **not** CI-approved or safe to release. Never push unrelated or unreviewed work to force a five-task total.
- **Batches are independent planning units.** Worker A may implement a task in Batch F while B's Batch D is running CI and C starts a task in Batch H, provided file ownership/dependencies do not overlap. A task needs a single atomic claim, not a completed previous batch. CI only gates the **publish/merge/release of the affected commit**, never unrelated local work.

## Three slots, no shared working tree

| Slot | Worktree relative to repo | Exact branch | Exclusive allowed source families | Existing work |
|---|---|---|---|---|
| **Chat A** | `.agent-worktrees/chat-a` | `agent/chat-a-b39-save-recovery` | `ui/Editor.kt`, `ui/SidePanel.kt`, `format/**`, `platform/**` and matching tests | Retains P0 save/recovery and legacy LANE-A claim |
| **Chat B** | `.agent-worktrees/chat-b` | `agent/chat-b-g08-toolbar-accessibility` | `ui/Toolbar.kt`, `ToolbarFrame.kt`, `ToolbarCustomizer.kt`, `FloatingToolbar.kt`, `InfiniteToolbar.kt`, `CanvasQuickAccess.kt`, `Tooltip.kt`, `test/.../ui/**` | Retains G08 accessibility and legacy LANE-B claim |
| **Chat C** | `.agent-worktrees/chat-c` | `agent/chat-c-core-canvas-reliability` | `core/**`, `canvas/**` and matching `test/.../core/**`, `test/.../canvas/**` | New independent input/eraser/geometry/engine reliability lane |
| **Gemini** | Existing sibling worktree (outside LocalMCP root) | Existing Quick Scratch branch | Existing reserved NP-004 UI files | **Not a fourth GPT writer**; do not take over |
| **Coordinator** | Top-level repository checkout | Dedicated local `integrate/**` branch | Shared strings, trackers, `AGENTS.md`, CI/release, `MainActivity.kt` and other shared files | Single serialized integrator, not a permanent fourth background agent |

**One-time Chat C provisioning (coordinator only, after the plan is published):** `python tools/agents/provision_chat_c.py`. It checks the exact remote `developer` head, creates a pinned base ref and the isolated C worktree only if absent, extends the existing local pre-commit/pre-push hooks to C without replacing custom hooks, and tests C's ownership guard. It stops instead of deleting/recreating an occupied path. Then run `python tools/agents/triad.py init` to seed the 130 tasks and current A/B legacy sessions; it does not overwrite existing claims. A newly opened Chat C must explicitly register its **own unique session** using `triad.py replace --slot C --session C-unique --reason "Provisioned the first C conversation"` (omit `--previous` only on its initial join). For a new computer or clone, first verify worktrees, refs and remote state; do not blindly rerun setup.

These are **strict file-ownership limits** enforced cooperatively by `tools/agents/check_lane.py`. A routing label (A/B/C) in the A-Z queue **does not expand permissions**. An NP card requiring `Backstage.kt`, `PreferencesPane.kt`, a shared string, or another lane's file must be paused for coordinator scope allocation. Never bypass the ownership guard or edit in the administrative checkout as a worker. Chat C's pinned baseline is `refs/heads/agent/chat-c-start-20261115`; do not move it after provisioning. Shared resources and default branch remain coordinator-only.

## Before every task, in every chat

1. Confirm LocalMCP `system_info` and its allowed root; read **the top-level** `AGENTS.md`, this protocol and the task JSON, plus your lane's `STATUS.md`. Existing A/B branches may contain old policy files: consult the canonical administrative files / latest `origin/developer` for newer instructions **without merging into your worker branch blindly**.
2. Confirm `git -C <your-worktree> status --short --branch`, `rev-parse HEAD`, exact assigned branch, and `git ls-remote origin refs/heads/developer`. Inspect the existing legacy coordinator `status` before touching its claimed source files.
3. Use a **slot session**. `triad.py` checks session identity on every claim, heartbeat, handoff, and CI incident; use a unique session ID when starting a newly opened chat. An old session ID is fenced out after replacement. An unchanged worktree is not proof a ChatGPT conversation is still running.
4. Call `next --slot A/B/C --session <current>` to see eligible tasks. Explicitly **claim** exactly one before editing. A P0/P1 task belongs to the next planning batch as recorded; independent tasks elsewhere may proceed even if previous CI is running/offline.
5. Inspect callers, resources, tests, schema/format compatibility and the complete change scope. Do the smallest working fix, preserve all user data and current formats. No local Gradle/Kotlin/Android build, no APK install; run Python source checks, privacy checker, `git diff --check` and `tools/agents/check_lane.py` in the assigned worktree.
6. Commit **locally** only, update your own `docs/agent-work/chat-X/STATUS.md` and hand off the **exact 40-character worker HEAD** with evidence. Do not directly push, force push, switch other worktrees' branches, or mark hardware work done from source inspection. The coordinator reviews `handoff` and uses `approve` to mark `local_ready`, **not** `CI passed`.
7. Repeat the next eligible independent task on another user turn. Local progress never waits on a different batch's CI.

**CLI examples** (run these from the Windows top-level root for read-only commands; for worker mutations pass `--repo .agent-worktrees/chat-a` / `chat-b` / `chat-c`; the actual script is in the top-level checkout and may be invoked by absolute path from older worktrees):

```powershell
python tools/agents/triad.py status
python tools/agents/triad.py batch-ready A
python tools/agents/triad.py --repo .agent-worktrees/chat-a next --slot A --session legacy-a
python tools/agents/triad.py --repo .agent-worktrees/chat-a claim NP-071 --slot A --session legacy-a
python tools/agents/triad.py --repo .agent-worktrees/chat-a heartbeat --slot A --session legacy-a
python tools/agents/triad.py --repo .agent-worktrees/chat-a handoff NP-071 --slot A --session legacy-a --commit <40-char-SHA> --evidence "Sources and tests reviewed; exact local commit and limitations documented"
# Coordinator-only review (not a statement of CI/device success):
python tools/agents/triad.py approve NP-071 --evidence "Reviewed precise worker commit, ownership, source tests and pending device QA"
```

**The old LANE-A and LANE-B reservations still govern their active work.** The new task ledger is additive, not a release/ownership replacement. A/B must not claim a new NP task that conflicts with work held by legacy `coord.py`. The current independent C slot is available only after its worktree has been provisioned and verified.

## When the old chat finishes, a NEW chat replaces its slot

**Do not create a second writer worktree for the same role.** A new conversation becomes the successor of the old A/B/C slot **only by explicit coordinator replacement**, never just by copying the prompt. ChatGPT cannot observe another chat ending. The user must stop the old conversation or ensure it will make no further edits.

1. Old contributor commits or hands off its work and reports exact HEAD / task / pending issues. If the old chat is inaccessible, the coordinator inspects its existing worktree and Git state; **never discard changes or reset a branch**.
2. Confirm the old slot's recorded session with `triad.py status`, then run from the **administrative root** (no dirty worker tree):
   ```powershell
   python tools/agents/triad.py replace --slot A --previous legacy-a --session A-20261115-new --reason "User ended the previous A conversation and approved successor"
   ```
   For Chat C's **first connection**, omit `--previous` and supply a unique `--session C-...`; Chat B uses its current recorded `--previous` value. Session changes are compare-and-swap and atomically transfer active task and CI incident ownership to the new session. Old session calls are rejected.
3. New contributor uses the **same owned worktree, branch, task and status file**; reads previous handoff/commit and continues. It does not recreate branches, delete worktrees or start a competing claim.
4. **Important limit:** the session fence covers cooperative coordinator operations, not arbitrary LocalMCP file writes. Stop prompting the old chat and keep one authorized writer per slot. This is not a credential-based lock or automatic ChatGPT migration.

## CI and network failure - nonblocking and repairable by ANY of A, B or C

- An in-flight, queued, or network-failed CI run **must not prevent independent tasks being claimed, edited, source-checked and committed locally**. Each task can come from another planning batch. The coordinator never claims success because a job is queued or offline.
- GitHub CI is **required before publishing/merging another dependent verified head**. Only the publishing step pauses when the latest protected integration CI is unknown, failed or the network is unavailable; work continues on local branches. Never disable tests, add `continue-on-error`, spoof artifacts, change a workflow to greenwash a failure, or push blindly during outage.
- Any A/B/C chat may reserve a failed/infrastructure CI incident independently:
  ```powershell
  python tools/agents/triad.py --repo .agent-worktrees/chat-c ci-claim 43679116911 --slot C --session C-current --reason "Investigation of concrete runner or code failure"
  python tools/agents/triad.py --repo .agent-worktrees/chat-c ci-finish 43679116911 --slot C --session C-current --evidence "Root cause and verified remediation/retry plan with logs"
  ```
  Another chat cannot claim the **same incident**. Different incidents and unaffected feature tasks are independent. Coordinator uses `ci-close RUN --evidence ...` after review.
- **Infrastructure/network failure:** cite the real GitHub error, distinguish failure from source regression; use bounded retry or GitHub re-run of the **same SHA**, only when accessible. If CI is inaccessible, record `UNKNOWN/INFRA`, not `PASS`. Do not repeatedly repush the same commit just to retry.
- **Real source/test failure:** the claiming chat diagnoses it, creates a **minimal repair only in its owned paths** with regression tests, and hands off. If the fix belongs to another lane or shared path, coordinator transfers a precise file/task ownership window or makes a serialized integration-only fix. **Any chat can diagnose; none may overwrite another lane to repair.**
- **Single publisher:** an available A/B/C chat may act as the coordinator **only in the administrative checkout**, after acquiring the common SQLite `publish-lock` for the exact remote SHA and recording verified CI evidence. Other contributors keep coding while that chat integrates. A publish lock prevents concurrent cooperative pushes; after one fast-forward-only push, verify the remote SHA and `publish-release` with results. Do not steal active publish locks on stale heartbeats; investigate and coordinate explicit recovery.

```powershell
python tools/agents/triad.py publish-lock --slot B --session B-current --base <current-developer-40-char-SHA> --evidence "Exact prior developer SHA and successful GitHub CI run independently checked"
# Review all five tasks (or explicit deferrals), changes, conflicts, tests and current remote SHA; guarded fast-forward only.
python tools/agents/triad.py publish-release --slot B --session B-current --evidence "Fast-forward complete, remote SHA checked; new run awaits GitHub CI"
```

**Never promise the chats will execute autonomously:** ChatGPT conversations and LocalMCP do not run continuously without a new user turn or a separately enabled scheduler. One ChatGPT chat cannot send prompts into another from this protocol.

## Evidence and final APK gate

Track four independent states: **local reviewed**, **published to developer**, **exact-SHA GitHub CI passed**, and **physical device accepted**. A green debug artifact is not a signed production release. Required checks include old native-note fixtures, low-storage/force-stop and SAF failures, stylus/palm/eraser, rotation/200-page PDF, TalkBack, full-ABI runner, signing and rollback plan. These are release cards `REL-01` through `REL-20` in Batches W-Z, with manual evidence recorded by the coordinator; never claim device success without an actual device session.

A protected release requires reviewing main/master-only changes and preserving rollback tags, selecting an existing matching tag and release key, and verifying a signed artifact and SHA on its actual target commit. **Do not promote or force-update `main`/`master` through this planning workflow.**

## Source of truth and recovery

- **Feature requirements:** `docs/notein-parity/FEATURE_MATRIX.md` (110 NP IDs).
- **Test procedures:** `docs/notein-parity/TEST_PLAN.md` (79 NT IDs).
- **Versioned A-Z queue:** `docs/github/APK_BATCH_A_TO_Z.md` and JSON task snapshot.
- **Live local claims:** common Git `.git/agent-coordination/claims.sqlite3`, `triad_*` tables for A-Z sessions/task/incidents/publication. Existing `claims` and `events` tables are never replaced.
- **Individual evidence:** `docs/agent-work/chat-a/STATUS.md`, `chat-b/STATUS.md`, `chat-c/STATUS.md`, plus CI URLs and SHA, and `docs/notein-parity/PROGRESS.md`.
- **Reclone limitation:** SQLite runtime claims, local Git hooks, unpushed commits, ChatGPT session existence, and user-machine worktrees are **not synchronized by GitHub**. If moving computers, export/review a signed-off handoff manifest and recreate safe worktrees/claims. Do not infer that the versioned JSON file contains live claim status.
- Run `python tools/agents/make_apk_backlog.py --check` to validate all 130 task definitions and 79 NT mappings. `--write` is a **planning migration**, not a status reset; never rerun it on a live queue without review.
