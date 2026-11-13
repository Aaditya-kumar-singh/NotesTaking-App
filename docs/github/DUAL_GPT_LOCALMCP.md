# Two GPT chats on one NotesTaking App repository (LocalMCP)

**Setup date:** 2026-11-13. **Integration target:** `developer`. **Baseline:** exact published B38 commit `b2e38f37cc0518c3886f3712951715a4b0d04c84`.
This plan is for **two separate ChatGPT conversations using one LocalMCP server**. It does not create a second MCP connection or allow one conversation to control another. Both chats share a Git object database, **not** a working tree, index, or branch. Each chat must be run on a user turn; nothing happens asynchronously.

## 1. Where each chat works

| Actor | Root or checkout | Branch | Ownership |
| --- | --- | --- | --- |
| **Chat A = this ChatGPT conversation** | `C:\Users\WHITE-SNAKE\Desktop\xnotes-android\.agent-worktrees\chat-a` | `agent/chat-a-b39-save-recovery` | **B39 / G03**: P0 native-save recovery, interrupted SAF writes, NP-081 / NP-082 / NP-084. Inspect first; fix only a concrete safe source bug, with regression cases. |
| **Chat B = second ChatGPT conversation** | `C:\Users\WHITE-SNAKE\Desktop\xnotes-android\.agent-worktrees\chat-b` | `agent/chat-b-g08-toolbar-accessibility` | **G08 toolbar accessibility**: NP-089 / NP-090 / NP-092, starting with toolbar touch-targets, spoken selection, and small-screen/large-text UI. No storage changes. |
| **Coordinator = Chat A when integrating** | `C:\Users\WHITE-SNAKE\Desktop\xnotes-android` | `chore/localmcp-two-gpt-lanes-20261113` at setup | Review the two branches, update shared NP/NT trackers, serialize Git publishing and CI; **never write feature code here while lanes are active**. |
| **Gemini (separate, previously assigned)** | `C:\Users\WHITE-SNAKE\Desktop\NotesTaking-QuickScratch` | `feature/gemini-np004-quick-scratch` | Reserved **NP-004 Quick Scratch**. No code/commits were detected at setup. Gemini is outside LocalMCP's allowed file root and is **not one of the two GPT lanes**. |

**LocalMCP access constraint:** the server's allowed root is the main `xnotes-android` directory. Direct `mcp__localmcp__read_file` / `edit_file` / `search_code` calls cannot reach sibling worktrees outside that root. Therefore Chat A and Chat B use **actual worktrees inside the allowed root**. These are ignored by the primary checkout via its local `.git/info/exclude`; they remain normal independent Git worktrees.

**Never** tell both GPT chats to use the default MCP working directory. That would put them back in the same checkout and permit branch switches, overwrites, and index conflicts.

## 2. File ownership: explicit and mutually exclusive

Chat A may change only:
- `app/src/main/java/com/notestaking/app/ui/Editor.kt` and `ui/SidePanel.kt`;
- `app/src/main/java/com/notestaking/app/format/**` and `platform/**`;
- `app/src/test/java/com/notestaking/app/format/**` and `platform/**`;
- `docs/agent-work/chat-a/**` for its own status and acceptance evidence.

Chat B may change only:
- `app/src/main/java/com/notestaking/app/ui/{Toolbar.kt,ToolbarFrame.kt,ToolbarCustomizer.kt,FloatingToolbar.kt,InfiniteToolbar.kt,CanvasQuickAccess.kt,Tooltip.kt}`;
- `app/src/test/java/com/notestaking/app/ui/**`;
- `docs/agent-work/chat-b/**` for its own status and acceptance evidence.

**Reserved for Gemini:** `ui/Backstage.kt`, `ui/NewNoteScreen.kt`, `ui/ExplorerChrome.kt` and their creation-flow tests. **Shared/integration-only:** `AGENTS.md`, `docs/notein-parity/{PROGRESS,BATCHES,FEATURE_MATRIX,TEST_PLAN}.md`, `app/src/main/res/values*/strings.xml`, `app/src/main/java/com/notestaking/app/MainActivity.kt`, CI/release files, Gradle, `tools/agents/**`, and this policy.

If a task truly requires a forbidden path, **stop, report the exact file and why it is needed, and ask the coordinator to transfer ownership or make a tiny integration commit**. Do not silently expand the allowlist. Existing resource strings may be reused without editing the shared translations. Do not cherry-pick other agents' changes into a feature worktree just to get access to their files.

## 3. LocalMCP tool path rule (critical)

In both conversations, first call LocalMCP system-info to confirm it is connected and its allowed root is `xnotes-android`. Then:
- **Read/edit:** pass `.agent-worktrees/chat-a/app/.../File.kt` or `.agent-worktrees/chat-b/app/.../File.kt` as `file_path` (never `app/.../File.kt` alone).
- **Search:** set `dir_path` to `.agent-worktrees/chat-a/app/...` or `.agent-worktrees/chat-b/app/...`.
- **Shell/Git/check scripts:** use `mcp__localmcp__bash({command: "...", cwd: ".agent-worktrees/chat-a"})` or the `chat-b` equivalent. If the tool does not honor `cwd`, use `git -C .agent-worktrees/chat-b ...` / `Push-Location .agent-worktrees/chat-b` inside that single command; do **not** permanently change the MCP server's root.
- A file under the administrative `xnotes-android/app/**` is **not** the same file as the one under `.agent-worktrees/chat-b/app/**`, even though both have the same relative project layout.

## 4. Enforced guard and local Git hooks

`python tools/agents/check_lane.py` **fails closed** unless run from the correct lane worktree, on its designated branch, with only permitted changed paths. It checks **committed changes since the coordination baseline, staged changes, unstaged changes, and untracked files**. The installed local `pre-commit` hook invokes this guard automatically for each lane, preventing accidental commits of unowned files. The local `pre-push` hook blocks pushes directly from either lane checkout; **the coordinator alone publishes**.

These hooks live in the common local `.git/hooks`, are **not versioned**, and must be reinstalled after a fresh clone. Hook bypass, shared-path edits made from the administrative checkout, and external tools remain possible: this is strong cooperative isolation, **not a security sandbox**. Code review, guard outputs, exact remote SHAs, and CI are still required.

Test the guard inside each lane:
```powershell
python tools/agents/check_lane.py
python tools/ci/check_project.py
python tools/ci/check_backup_privacy.py
git diff --check
git status --short --branch
```
No local Gradle, Kotlin tests, lint, NDK/Android build, APK install or device run: the repository's existing no-local-build policy still applies.

## 5. Per-turn rules for each GPT

1. Read `AGENTS.md`, this policy, the original Local First/Batched CI policy, and your own `docs/agent-work/chat-?/STATUS.md`.
2. Confirm exact `git rev-parse --show-toplevel`, branch, `git status`, and latest `origin/developer` **without switching to it**.
3. Inspect real code and every caller before fixing one bounded task; never invent a test result.
4. Change **only owned paths**. Record task ID, touched files, test cases, commit SHA and blockers in your own `STATUS.md`.
5. Run the lane guard, Python source/privacy validators and `git diff --check`. Make small **local-only** commits. If the hook blocks a commit, do not bypass it.
6. Report `LOCAL READY`, not `CI PASSED`. Neither chat may push, force-push, alter `developer`/`main`/`master`, delete worktrees, or rewrite history.

## 6. Single-writer integration (Chat A as coordinator)

1. Confirm prior `developer` CI is green on the exact current remote SHA. Confirm the other lane is not editing a shared path and obtain both local commit SHAs/status files.
2. Verify source checks, test additions, `git diff --name-status <coordination-branch>..<lane-branch>`, ownership guard and all new/changed callers. Ask the task owner to fix any failure **on their lane**, not by overwriting the other lane.
3. Integrate one lane at a time in the **administrative coordinator checkout**. Preserve its untracked `.kotlin/` and `home_run_gsheet_export.py`. Start from the coordinator baseline, then cherry-pick the reviewed lane-only commits in order. If a conflict occurs, **stop** and resolve explicitly; no blind `git merge -X ours/theirs`, mass staging, forced reset or force-push.
4. Update shared `PROGRESS.md`, `BATCHES.md`, `FEATURE_MATRIX.md`, and `TEST_PLAN.md` **only here**, reflecting exact evidence. Keep the detailed lane `STATUS.md` history for audit.
5. Perform full permitted source checks and compare the fetched `origin/developer` SHA with the expected SHA immediately before one **fast-forward-only** push to `developer`. If the remote moved, **stop and re-review/reconcile**, never overwrite it.
6. Let GitHub Actions validate that precise pushed SHA. Do not merge/publish the second lane until its dependency graph is reconciled and the previous checkpoint's CI is resolved. Physical phone/tablet tests remain separate and must be recorded before declaring the feature accepted.

**Normal order:** coordinator plan commit -> first finished lane (A or B) -> verified `developer` CI -> second lane cherry-picked onto the updated integration tip -> next CI. Both agents may keep working locally while CI runs.

## 7. Local recovery and handoff

- Chat A status: `docs/agent-work/chat-a/STATUS.md`.
- Chat B status: `docs/agent-work/chat-b/STATUS.md`.
- Chat B's copy/paste starter: `docs/github/GPT_CHAT_B_STARTER.md`.
- Common guard: `tools/agents/check_lane.py`.
- For a newly cloned repo, recreate the two worktrees and install the local hooks **only after inspecting the current branches and any existing worktrees**; never re-run setup blindly or delete an existing worktree. See exact initial worktree branch names above.

Do not share the two chats' conversation IDs, credentials, private notes, or tokens in Git. The only handoff is via reviewed commits and the two status files.
