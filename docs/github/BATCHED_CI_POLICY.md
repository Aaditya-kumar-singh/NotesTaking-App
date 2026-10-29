# Batched GitHub CI policy

**Scope:** NotesTaking App development on the `main` integration branch. The Windows machine is for editing, source inspection, and dry-run checks only: **no local Gradle, Kotlin test execution, lint, native compilation, APK builds, or device installation**. **Running CI must never pause independent local development**; follow `LOCAL_FIRST_PARALLEL_WORKFLOW.md` to manage parallel work and the next task.

## When an Android build runs

| Event | Source checks | Kotlin tests, lint, and debug APK |
| --- | --- | --- |
| Commit locally on a feature/fix branch | No | No |
| Push a feature/fix branch without a PR | No | No |
| Open or update a **draft** PR into `main` | Skipped | Skipped |
| Mark draft PR **Ready for review** | Yes | Yes, ARM64 for UI-only changes; all three ABIs for native/build changes |
| Open a non-draft PR into `main` | Yes | Yes, ARM64 for UI-only changes; all three ABIs for native/build changes |
| Push more commits to a **ready** PR | Yes, latest PR commit | Yes, latest PR commit (native/build changes select all three ABIs) |
| Manually run **Android CI** and select a branch | Yes | ARM64 by default; select `full_abis` to build all three |
| Merge into `main` | Yes (post-merge) | Yes, **all three ABIs** (post-merge) |
| Signed release | Separate manually dispatched release workflow | Separate, requires signing configuration |

A draft PR may still show a **skipped** GitHub Actions run. No Android runner is started. Ready PRs still run Kotlin tests, lint, native ARM64 compilation, and APK verification. The production release and integrated `main` continue building ARM64, ARMv7 and x86-64. **When a PR changes C/C++, JNI, Gradle, the Android toolchain or the CI verifier, the ready PR automatically runs a full-three-ABI GitHub build and APK validation before merging.**

## Work in batches

1. Start a `feature/` or `fix/` branch based on updated `main`. Make related changes in one batch (for example, all dialog surfaces, then the canvas toolbar). Keep unrelated audit/security fixes in their own batch.
2. Inspect affected callers, resources, tests, and compatibility. Run only permitted **source-level** dry runs such as `git diff --check`, resource reference checks, and the Python source validator. Neither a dry run nor a passing static check is an Android build.
3. Commit during the batch as necessary, but avoid repeated pushes solely to test unfinished UI. Push a branch when remote backup or review is helpful. A branch push by itself **does not** start Android CI once this policy is present in that branch's workflow.
4. Optionally open a **draft** PR and continue making changes without an Android build. Before declaring it ready, review the entire diff, preserve user data formats, add regression tests for nontrivial changes, and record any QA limits.
5. Push the complete batch and mark the PR **Ready for review**. This runs the **Source, resources and attribution** job, then the full **JDK 17 / tests / lint / debug APK** job in GitHub Actions. If a ready PR changes afterward, GitHub reruns these checks to verify its new head.
6. When a checkpoint APK is genuinely needed, open **Actions → Android CI → Run workflow**, select the feature branch and run it **after** pushing the batch. The default feature-branch APK targets ARM64. Select **full_abis** to request all three architectures manually; ready PRs already auto-detect native/toolchain changes and build all three. This manual option will only be exposed reliably after the updated workflow is available on GitHub's default branch; switch the repository default from `master` to `main` after reviewing its settings. Record the tested commit SHA. Manual verification of a branch tip does not substitute for required PR merge checks where the PR merge commit differs.
7. Merge to `main` only after all required checks pass on the current PR, plus review of any runtime/device risks. A `main` push triggers one post-merge safety run. Production signing remains a separate manual release gate.

## Rules that prevent false green results

- **No** `continue-on-error` or removed assertions to make a failing build look successful.
- **No** unchecked direct push to `main`; use branch protections requiring the existing source and Android-build statuses. The connector cannot currently verify branch protection because GitHub denies access; confirm it in repository settings.
- **No** automatic Android build from every feature/fix push. Do not delete `pull_request.synchronize`: that event ensures changes to a ready PR are retested.
- **No** local Gradle builds/tests. If a test is needed, use GitHub Actions on the selected branch.
- **No** production release merely because unit tests pass; device checks of stylus/palm rejection, dialogs, backup/save, and PDF handling remain separate acceptance requirements.
- **No** treating ARM64-only debug APKs as universal builds; x86-64 emulators and 32-bit ARM devices require a full-ABI checkpoint.
- Release signing and F-Droid builds do **not** pass `ciArm64Only`; the default Gradle configuration remains the original three ABIs.

## Compatibility with existing branches

This policy is effective when the branch being pushed **contains the updated** `.github/workflows/android-ci.yml`. Long-lived branches made before the policy may still have the older push triggers, because GitHub evaluates workflow configuration from the pushed ref. Bring those branches up to date with `main` after the policy lands, or cherry-pick the workflow-only change without mixing unrelated features. The repository currently reports `master` as the default branch; switch that separately after reviewing branch rules and release workflows. Manual workflow dispatch remains available because Android CI already exists on the default branch.

The policy deliberately keeps one post-merge Android run on `main` as a safety check until main-branch protection is confirmed. Removing it prematurely could allow direct or unreviewed pushes to escape Android verification.
