# Local-first development + parallel GitHub verification

**Project:** NotesTaking App, native Kotlin/Jetpack Compose Android.
**Updated:** 2026-10-29.
**Rule:** **Development continues during CI.** GitHub CI is an acceptance gate for a particular commit, not a signal to stop writing the next feature.

This document is the operating plan for future `next` / `proceed` requests. It complements [BATCHED_CI_POLICY.md](BATCHED_CI_POLICY.md) and [WORKFLOWS.md](WORKFLOWS.md), which define the actual GitHub triggers.

## 1. Two work lanes

| Lane | Runs whenever | Work | Output |
|---|---|---|---|
| **A — Local development (continuous, on user turns)** | Regardless of CI status | Inspect the next unblocked requirement; edit a separate local branch; check callers, tests and compatibility; run source-only dry-runs; commit coherent checkpoints | Locally committed changes, updated task queue; **no push per change** |
| **B — GitHub verification (independent)** | A complete published batch is marked ready | GitHub-hosted Kotlin compilation/tests, lint, native build, APK verifier and security checks, then device QA/review | Evidence for an exact SHA; fix/merge decision, **not** a development pause |

The two lanes overlap. While PR X is building, continue task Y locally on another branch. Do not change the published PR X head until its current run is resolved, except for a targeted fix to a confirmed failure. No automated or unattended future activity is implied: each user turn makes progress through the available tools.

## 2. On every future `next` request

1. **Orient (briefly).** Check current branch, dirty/untracked files, `origin/main`, ready PRs and active CI once. Reuse the already-inspected backlog; don't repeatedly poll a long build in place of development.
2. **Pick the highest-priority *unblocked* task** from Section 5. Existing failing CI with actionable output takes priority for the affected PR; otherwise continue local development.
3. **Isolate the work.** Create/use one local branch for a coherent batch. If it depends on an unmerged PR, branch from that PR's known head; record that parent SHA. For independent safety/security/CI work, branch from `origin/main`. Avoid mixing reliability fixes with UI redesign.
4. **Implement the smallest complete change.** Read the actual code, its callers, resources, and tests first. Preserve drawing semantics, note formats, settings migration, licenses and private data. Include regression tests in source where appropriate; executable Kotlin tests remain on GitHub.
5. **Validate locally without building.** Run `python tools/ci/check_project.py`, `python tools/ci/check_backup_privacy.py` when relevant, targeted static assertions/resource checks, and `git diff --check`. These do **not** establish that the Android app compiles.
6. **Commit locally**, update the task status/evidence, then keep working on the next unblocked item if the user asked to proceed. Report branch, local SHA, checks, and **push: no**.
7. **Publishing checkpoint.** When the coherent batch is ready *and* the previous requested GitHub CI checkpoint is resolved, inspect its complete diff, reconcile with latest `main` and push once. If rewriting an existing branch is unavoidable, preserve a local backup and use a guarded `--force-with-lease` for its known old SHA. Never silently overwrite someone else's remote work.
8. **CI/review/merge.** Ready PR checks run on the published commit; verify their exact SHA and source, Kotlin, lint, APK and security results. UI/device-critical work additionally requires tablet/phone screenshot and real stylus/TalkBack acceptance. Merge only after applicable gates. If green, continue with the already prepared next local batch instead of beginning planning from zero.

**If CI is still running:** skip Steps 7–8 for that branch and **keep doing Steps 2–6** on the next local task. An independent completed branch may be committed locally while another runs in GitHub.

## 3. Branch and release discipline

- `main`: GitHub integration branch. No direct unverified push or merge without checks. GitHub's **default branch is still `master`**, which needs an admin-reviewed change; don't change it automatically.
- `feature/modern-paper-ink-foundation`: published **PR #8** at SHA `e181918`, ready for review; **do not add the next feature to this live CI branch**.
- `feature/post-brand-home-polish`: **local next-batch branch** created at the PR #8 head. Work on Home/Explorer/empty-state UI here while its parent PR builds; it should not be pushed yet. After PR #8 merges, use `main` as the updated base (cherry-pick/rebase only this branch's new commits); keep earlier PR contents out of the next PR.
- `chore/codeql-compile-only`: **separate local** CI optimization commit `a979652`, based on `origin/main`; publish and verify separately, after the current UI CI checkpoint. Compile-only CodeQL coverage remains **unverified** until GitHub proves extraction and findings upload.
- Every new batch must have one owner branch, a parent SHA, a bounded purpose, source-only checks, and a clear status. Multiple local branches are fine; simultaneous modifications to a live ready PR are not.
- Work on different paths can be parallelized conceptually, but the single local working tree needs an explicit branch switch with a clean tracked index. Preserve the existing untracked `.kotlin/` directory.

## 4. Gates and evidence

| State | Meaning | Safe next action |
|---|---|---|
| **Planned** | Prioritized but not edited | Inspect and start locally |
| **Local implementation** | Code edited, checks may be pending | Complete source checks |
| **Local ready** | Scoped code committed; source-only checks pass | Continue another local task or prepare one batched push |
| **Published / CI running** | Exact branch SHA pushed, ready PR building in GitHub | Leave this PR head alone; work on separate local branch |
| **CI failed** | Specific failure in GitHub logs | Apply focused fix, publish one correction, rerun |
| **CI passed** | Required checks succeed on the **current PR SHA** | Review diff/device QA; merge if all required gates met |
| **Device QA pending** | APK is green but tablet/stylus/contrast/UX not checked | Test on real device; do **not** claim release ready |
| **Merged** | Approved and verified code on `main` | Reconcile dependent next-batch branches |

Never mark the 45 historical Excel audit items `Closed` based only on source commits or CI. Source-level passing is not a substitute for Kotlin compilation, device behavior, or an actual save/recovery test.

## 5. Prioritized task queue

This is a **snapshot**, not a background monitor. Refresh statuses when starting each user turn; keep unverified results pending.

| Priority | Task and scope | Track | State / next action |
|---|---|---|---|
| P0 — Release gate | PR #8 Paper & Ink visual foundation, popup/menu, toolbar accessibility, Settings reset and preview | Published PR #8 | Android CI at `e181918` running when this plan was created. Inspect any failure, later run tablet/phone visual and TalkBack QA; no merge on CI alone if device QA is required |
| **P1 — Work now** | **Home + Explorer + Empty states (UI-19/20/21)**. Improve readable hierarchy, consistent selection/focus, compact empty text and clear next action; preserve file operations | **Local `feature/post-brand-home-polish`** | **Next implementable task while PR #8 runs**. Inspect `Backstage.kt`, `ExplorerTiles.kt` and existing empty-state handlers before editing |
| P1 | Settings/toolbar appearance and 48dp touch-target accessibility on phone/tablet; assess color contrast, no paint-engine changes | Next UI batch | Backlog after Home/Explorer; combine with UI accessibility QA where safe |
| P1 | CodeQL: replace duplicate `assembleDebug` with JVM compilation and validate findings | Local `chore/codeql-compile-only`, commit `a979652` | Source-only checks passed; keep separate from UI. Publish for GitHub CodeQL verification at a later checkpoint; don't claim time savings until measured |
| P1 — Reliability | XN-033/034 stylus/palm rejection, XN-035 area eraser, XN-036 undo/redo | Existing fix branches | Review branch diffs against `main`; keep separate from cosmetic UI. Tests/hardware needed before closing findings |
| P1 — Audit | 22 integrated candidates + 17 remaining unaccepted findings from the 45-row register | `docs/audit/xnotes-problems-master.xlsx` | Reproduce, validate and record evidence individually; do not wholesale close rows |
| P2 | Typography (UI-06), progress/feedback (UI-17/18), complete theme/contrast matrix | Future batches | Follow after the immediate UI batch and real screenshots; don't add unnecessary new fonts |
| P2 — Admin | Default branch `master` → `main` with reviewed rules, release and GitHub Actions settings | GitHub settings | Requires repository admin; not a code change |

## 6. Example of how the next work proceeds

- **Now:** PR #8 runs in GitHub. On its own local child branch, inspect Home/Explorer and complete an independent UI improvement; save local commit(s) without pushes.
- **While the build continues:** Move to the next local item, or prepare source-level regression assertions. Switch to an independent local branch if the next item touches unrelated code.
- **If PR #8 fails:** Prioritize fixing the specific GitHub error on its PR branch without discarding the next local batch.
- **If PR #8 passes:** Review device acceptance before merging. Once merged, rebase/cherry-pick the post-brand Home/Explorer-only commits onto `main`; push as **one batch** when ready for its own PR/build.
- **Never:** stare at a running CI job, run expensive local Android builds, push every commit, hide CI errors or treat unverified device behavior as done.
