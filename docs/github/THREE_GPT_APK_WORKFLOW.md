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
- **Automatic batch tick:** The [A-Z completion checklist](APK_BATCH_PROGRESS.md) and the checkbox under each batch in [the task list](APK_BATCH_A_TO_Z.md) show `[x]` only after **all five tasks** are accepted by the coordinator (`verified` after successful exact-SHA GitHub CI, or `device_verified` after real device evidence), or an optional task is explicitly `deferred`. Unreviewed local commits, CI-running/CI-failed tasks, and `local_ready` tasks keep `[ ]`. The summary displays verified vs deferred counts so a deferral never looks like an implemented feature. The mark updates automatically after coordinator `verify`, `manual-complete` or `defer`; workers never edit progress checkboxes. The files are local snapshots until included in the next reviewed `developer` push; live state is `triad.py status`. Coordinator can recover a stale snapshot with `python tools/agents/triad.py checklist --write` (no new Android CI just to tick). A completed batch is still not an overall signed APK release.
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
# After the code is integrated and GitHub CI passes on the exact SHA, and
# applicable physical-device evidence has been reviewed:
python tools/agents/triad.py verify NP-071 --ci-sha <exact-40-char-SHA> --ci-run <run-id> --evidence "Exact SHA build, regression tests and required device acceptance evidence reviewed"
# A batch's [x] appears automatically only when all five task cards are resolved:
python tools/agents/triad.py batch-ready A
python tools/agents/triad.py checklist
```

**Important: a legacy `LANE-B: review` is not a freeze on independent A-Z coding.** The legacy pilot `coord.py recommend/claim-next` still enforces a single-lane capacity lock and must **not** be used to claim the next A-Z item. Instead, preserve the original review claim and immutable handoff SHA, then claim one new *independent* A-Z task with `triad.py claim` in the **same existing B worktree**. The new coordinator enforces a clean worktree and the reviewed checkpoint as an ancestor of current HEAD before allowing the claim; it does **not** change `LANE-B` from `review` or grant additional file permissions. Commit subsequent tasks on top of the intact handoff, and record separate exact SHAs. The coordinator may integrate/retest Checkpoint 44 first and review later descendants afterward, in sequence. Prefer a new task that does not edit the same file as an unintegrated checkpoint; if overlap is unavoidable, document the dependency and let the coordinator review the full commit chain instead of silently rewriting the reviewed patch. `reactivate_chat_b.py` now accepts an unchanged *ancestor* handoff and a clean descendant worktree after the exact-SHA CI gate succeeds. The release gate remains strict, while the development lane continues.

**LANE-A active** likewise remains a separate legacy reservation; its current owner may claim A-Z tasks in its own file scope. A *new* conversation must first complete explicit `triad.py replace` with the previous chat stopped; never treat an old `legacy-a` session as permission for two simultaneous A writers. The new ledger does not replace original A/B/Gemini ownership or device QA gates.

## Optional autonomous local worker process

The [A/B/C auto-dispatcher](AUTO_DISPATCHER.md) is a separate **opt-in local Gemini CLI process**, not ChatGPT browser automation. It can pick, atomically claim, source-check, locally commit and hand off successive eligible tasks across three isolated worktrees. It can reserve a narrowly preapproved coordinator-owned read-only UI audit for idle B without changing file ownership. It never uses browser sessions; the user must first stop those chat writers, explicitly replace each session with a unique `auto-a-*`/`auto-b-*`/`auto-c-*` token, and configure/trust an authenticated headless model. The live machine's Gemini CLI currently fails authentication (UNSUPPORTED_CLIENT), so no autonomous coding is running. CI/device acceptance, CI incident approval, merges and publishing remain separately gated. Running the script without `--execute` only previews tasks; with `--execute`, workers must obey the same Git hooks and ownership checks. It does not install a service or run when the Windows process is stopped.

## When a chat has no remaining eligible tasks

The original 130-task A-Z catalog is immutable. If a slot has submitted its work, the coordinator may **allocate a still-pending task** without changing its batch or giving away another worker's files. Only tasks in `pending` state can be allocated; tasks assigned to Gemini, another agent or device QA cannot be taken over. Allocations are recorded atomically in the live SQLite ledger; an allocation is **not a claim**, and the worker must claim it with its own registered session. The original catalog `owner` field remains unchanged, while the live task row records the reserved slot and exact scope.

Example for B: `python tools/agents/triad.py allocate NP-019 --slot B --scope "Read-only create menu audit of coordinator-owned Backstage/NewNoteScreen; add regression tests only under app/src/test/java/com/notestaking/app/ui/; do not edit shared production UI, A/C paths, or Gemini Quick Scratch"`. B may inspect out-of-lane source **read-only** and write only its own allowed UI test/status files. If a real fix needs `Backstage.kt`, `NewNoteScreen.kt`, strings or another lane's code, B reports the exact dependency; the coordinator applies or assigns that production fix separately. The existing `tools/agents/check_lane.py` remains authoritative. For C, `allocate NP-036 --slot C --scope "..."` prioritizes an already C-owned optional task without stealing another claim.

A reserved task appears before unreserved eligible tasks in `triad.py next`; use `--include-optional` for P2/P3 tasks. The `triad.py status` output shows `reserved_allocations` and keeps `pending/review/local_ready/verified` counts honest. Five tasks still form an A-Z review batch, but each slot may work on independent tasks across batches. No previously reviewed commit is changed, no CI gate is bypassed, and nothing runs without the contributor's next user turn.

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

- An in-flight, queued, failed, or network-unavailable CI run **must not prevent independent tasks being claimed, edited, source-checked and committed locally**, even when a previous task's legacy checkpoint remains in `review`. Each task can come from another planning batch. Leave all existing handoff and A-Z `review` states intact while working on a separate task; only one `active` task per slot, but previously handed-off tasks may remain under review. The coordinator never claims success because a job is queued or offline.
- GitHub CI is **required before publishing/merging another dependent verified head**. Only the publishing step pauses when the latest protected integration CI is unknown, failed or the network is unavailable; work continues on local branches. Never disable tests, add `continue-on-error`, spoof artifacts, change a workflow to greenwash a failure, or push blindly during outage.
- Any A/B/C chat may reserve a failed/infrastructure CI incident independently:
  ```powershell
  python tools/agents/triad.py --repo .agent-worktrees/chat-c ci-claim 43679116911 --slot C --session C-current --reason "Investigation of concrete runner or code failure"
  python tools/agents/triad.py --repo .agent-worktrees/chat-c ci-finish 43679116911 --slot C --session C-current --evidence "Root cause and verified remediation/retry plan with logs"
  ```
  Another chat cannot claim the **same incident**. Different incidents and unaffected feature tasks are independent. Coordinator uses `ci-close RUN --evidence ...` after review.
- **Infrastructure/network failure:** cite the real GitHub error, distinguish failure from source regression; use bounded retry or GitHub re-run of the **same SHA**, only when accessible. If CI is inaccessible, record `UNKNOWN/INFRA`, not `PASS`. Do not repeatedly repush the same commit just to retry.
- **Real source/test failure:** the claiming chat diagnoses it, creates a **minimal repair only in its owned paths** with regression tests, and hands off. If the fix belongs to another lane or shared path, coordinator transfers a precise file/task ownership window or makes a serialized integration-only fix. **Any chat can diagnose; none may overwrite another lane to repair.** A failed published batch remains unapproved; the CI-fix commit is reviewed and tested on a new exact SHA before dependents are published. Independent A-Z coding, including subsequent local batch tasks, does not stop because the repair is in progress.
- **Single publisher:** the administrative coordinator registers a separate `COORD` **publication identity** in the shared SQLite ledger, without taking over A/B/C coding sessions. Only that registered coordinator, working in the administrative checkout, acquires the common `publish-lock` for the exact remote SHA with verified CI evidence. Changing coordinator conversations requires an explicit compare-and-swap replacement and cannot happen while a publish lock exists. Other contributors keep coding while that chat integrates. **A worker having uncommitted changes is not a global publish blocker**: compare the coordinator commit's changed paths against the worker's uncommitted paths and preserve them; stop only on an actual ownership/path conflict or unsafe dependency. Never stash/reset another worker merely to publish an unrelated coordinator change. A publish lock prevents concurrent cooperative pushes; after one fast-forward-only push, verify the remote SHA and `publish-release` with results. Do not steal active publish locks on stale heartbeats; investigate and coordinate explicit recovery.

```powershell
python tools/agents/triad.py coordinator-register --session COORD-unique --reason "Start exclusive administration of reviewed developer integrations"
python tools/agents/triad.py publish-lock --slot COORD --session COORD-unique --base <current-developer-40-char-SHA> --evidence "Exact prior developer SHA and successful GitHub CI run independently checked"
# Review all five tasks (or explicit deferrals), changes, conflicts, tests and current remote SHA; guarded fast-forward only.
python tools/agents/triad.py publish-release --slot COORD --session COORD-unique --evidence "Fast-forward complete, remote SHA checked; new run awaits GitHub CI"
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
