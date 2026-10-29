# NotesTaking App — instructions for coding agents

Read [the local-first parallel workflow](docs/github/LOCAL_FIRST_PARALLEL_WORKFLOW.md) and [the batched CI policy](docs/github/BATCHED_CI_POLICY.md) before changing this repository.

- **Never wait for GitHub CI to finish before starting the next independent local task.** CI gates publishing, merging, and releases; it is not a gate for source editing. Check CI briefly at the beginning/end of a turn, then do real local work.
- Work in small, clearly scoped **local branches**, especially when a ready PR is currently building. Do not add unrelated commits to a branch under GitHub verification. A dependent branch may start at that PR's exact head; reconcile it onto `main` after the PR merges.
- **Do not push every commit.** Commit useful local checkpoints. Push a coherent, reviewed batch once, normally after the prior GitHub CI checkpoint completes, and record the exact SHA. Do not trigger an extra expensive build just to measure progress. No direct unverified pushes to `main`.
- On this Windows computer, **never run Gradle, Kotlin tests, Android lint, NDK/CMake builds, APK builds, or device installs**. Allowed: source inspection, edits, Git operations, and source-only Python/YAML/resource/diff checks. Actual builds and executable tests run only on GitHub Actions.
- Before editing, inspect callers, resources, tests and data compatibility. Do not change `.xnote`, `.xcanvas`, `.xtemplate`, original license/NOTICE, private notes, and drawing/storage behavior casually. The historical 45 audit findings are **Open** until real acceptance evidence supports closure.
- On each `next` or `proceed`: select the highest-priority **unblocked** item in the workflow task queue, implement a bounded change, run permitted dry-runs, commit locally, and report what was *actually* verified. If a GitHub run fails, prioritize its concrete error; continue independent local work in a separate branch if the fix is not yet available.
- Distinguish **implemented locally**, **pushed**, **cloud CI passed on exact SHA**, **device verified**, and **merged**. Never conflate these statuses. Do not promise unattended background work.

Keep the untracked `.kotlin/` folder untouched unless the user separately authorizes cleanup.
