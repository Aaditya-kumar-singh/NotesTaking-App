# Opt-in A/B/C auto-dispatcher (local computer)

`tools/agents/auto_dispatch.py` is a **local process**, not a way to wake ChatGPT browser conversations. It runs three independent **headless Gemini CLI** coding workers concurrently, using the same A/B/C Git worktrees and shared `triad.py` SQLite ledger. Workers only edit, source-check, commit **locally**, and hand off tasks for coordinator review. They never publish, merge, sign releases, approve CI, claim physical QA, or tick the A-Z batches.

This is **opt-in** and deliberately does **not** install a Windows startup task or take over existing browser sessions. `status` and `doctor` are read-only. Without an authenticated headless model the dispatcher cannot code unattended.

## Runtime checked November 15, 2026

- Windows has **Gemini CLI 0.68.0** and **Aider 0.86.2**. The dispatcher currently supports Gemini CLI as its coding backend.
- A real Gemini headless probe failed with **`IneligibleTierError: UNSUPPORTED_CLIENT`**. Its current login tier is not accepted, and the administrative directory was not trusted. No Codex or Claude CLI was found. Fix account/provider support and explicitly trust the relevant worktrees before executing. **Do not use `--skip-trust` or `--yolo` to suppress safety.**
- No model/API credential is stored in the code, prompt files or Git. By default Gemini is invoked with `--sandbox`; a compatible, functioning Gemini sandbox runtime is required for coding. Docker/Podman were not detected on this Windows machine at the last check. An explicit `--allow-unsandboxed` override exists for users who knowingly accept weaker isolation, but it is **not recommended** for simultaneous coding; source path guards detect mistakes after they occur and are not an OS security boundary.

## Dry-run and compatibility check

From the top-level Windows NotesTaking checkout:

```powershell
python tools/agents/auto_dispatch.py status
python tools/agents/auto_dispatch.py doctor --slots A B C
# Only after model access/workspace trust is available:
python tools/agents/auto_dispatch.py doctor --slots B C --probe-runtime
```

When Chat B runs out of its original tasks, the dispatcher can reserve one still-pending **coordinator-owned UI audit** from a narrowly preapproved list. B may *read* coordinator-owned UI and write only its existing owned UI regression tests and STATUS.md. Any real production UI fix remains coordinator-owned. Gemini/DEVICE/A/C tasks are never stolen. Chat C continues existing C-owned canvas/core work, including reserved NP-036.

## Exclusive takeover before autonomous coding

**Stop all old A/B/C browser chat conversations as coding writers first.** A CLI agent is a different writer and **cannot** reuse an existing browser session token. Verify no slot has an active, unfinished task and that all worker worktrees are clean. Never reset a worktree to force takeover.

Use `python tools/agents/triad.py status` to get each exact previous session, then transfer it explicitly:

```powershell
python tools/agents/triad.py replace --slot A --previous "<old A session>" --session "auto-a-local-01" --reason "Old A chat stopped; exclusive local auto-agent now owns A"
python tools/agents/triad.py replace --slot B --previous "<old B session>" --session "auto-b-local-01" --reason "Old B chat stopped; isolated headless B worker preserves reviewed handoffs"
python tools/agents/triad.py replace --slot C --previous "<old C session>" --session "auto-c-local-01" --reason "Old C chat stopped; isolated headless C worker preserves reviewed handoffs"
```

Use fresh unique session IDs (examples above). The dispatcher **never does this takeover automatically**. A/B legacy claims and Gemini NP-004 remain untouched. If the old chats are still working, do not run `--execute`.

## Run three concurrent workers once authentication is fixed

```powershell
# Preview only; no claims or edits:
python tools/agents/auto_dispatch.py run --slots A B C

# Executes at most five tasks in each lane:
python tools/agents/auto_dispatch.py run --execute --slots A B C --session A=auto-a-local-01 --session B=auto-b-local-01 --session C=auto-c-local-01

# Stays active and polls for new tasks as previous ones are handed off:
python tools/agents/auto_dispatch.py run --execute --watch --max-tasks-per-slot 0 --slots A B C --session A=auto-a-local-01 --session B=auto-b-local-01 --session C=auto-c-local-01
```

Run this foreground process on the Windows machine. `Ctrl+C` stops it; `--watch` polls while idle (default every 60 seconds). There is no ChatGPT task or remote scheduler keeping it alive. Each worker has its own SQLite connection and an OS-level slot mutex in `.git/agent-coordination/`.

Each worker: (1) verifies its explicitly registered `auto-a-*` / `auto-b-*` / `auto-c-*` session, correct clean worktree and no active task; (2) probes Gemini authentication/trust **before claims**; (3) atomically claims one eligible task; (4) runs Gemini with `--sandbox --approval-mode auto_edit` by default (never YOLO), with shell/Git/unowned edits explicitly prohibited; (5) checks current branch/HEAD, file ownership, Python source, privacy and Git diff; (6) commits only its own paths plus STATUS.md; (7) hands off the exact SHA and continues independent work. All Kotlin/JUnit, Android lint, APK and physical-device validation remain on GitHub Actions or real hardware.

If the model, tests, Git hooks or ownership check fail, the dispatcher **preserves files and the active claim** for manual diagnosis. It never resets, force-pushes, approves a failed build or fakes acceptance. Model output is held in temporary files while running, not committed.

**CI incidents:** Existing `triad.py ci-claim`/`ci-finish` allow any authorized A/B/C contributor to investigate a CI failure. The dispatcher deliberately does **not** merge, auto-retry failed CI or use a passing CI run as device acceptance. A coordinator must review and publish any fixes under the exclusive `COORD` lock.

## Safety and recovery

- Pre-commit/pre-push hooks, allowed path checks, SQLite claims and OS locks remain in force. OS locks prevent duplicate dispatcher instances, **not** all filesystem access by a remote model. The agents remain cooperative; a secure sandbox would require separate runtime isolation.
- The dispatcher is instructed not to create/switch worker branches or edit other lane files; it checks for those changes before committing. It cannot publish to developer/main/master or certify physical device tests. As with any LLM coding tool, OS sandboxing and a human review remain necessary to guard against an unintended edit before the post-run check.
- An interrupted active claim needs manual handoff or resolution before restarting. Do not delete working files.
- To return to browser ChatGPT coding, stop the dispatcher, ensure no active claim and clean worktrees, then explicitly `triad.py replace` to new browser session IDs.

**Validated:** dispatcher source-only tests and existing triad tests, dry-run status and queue selection. **Not executed:** actual autonomous coding, because Gemini CLI authentication is currently unsupported.
