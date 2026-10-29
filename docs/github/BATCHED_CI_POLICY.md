# Batched GitHub CI policy

**Scope:** NotesTaking App development on the `main` integration branch. The Windows machine is for editing, source inspection, and dry-run checks only: **no local Gradle, Kotlin test execution, lint, native compilation, APK builds, or device installation**.

## When an Android build runs

| Event | Source checks | Kotlin tests, lint, and debug APK |
| --- | --- | --- |
| Commit locally on a feature/fix branch | No | No |
| **Push a completed feature/fix/chore batch** (with this workflow on the branch) | Yes | **Yes, automatically**: ARM64 by default; all three ABIs when native/build paths changed |
| Open or update a **draft** PR into `main` | PR-event jobs skipped | PR-event jobs skipped; a separately pushed branch batch still runs its push CI |
| Mark draft PR **Ready for review** | Yes | Yes, ARM64 for UI-only changes; all three ABIs for native/build changes |
| Open a non-draft PR into `main` | Yes | Yes, ARM64 for UI-only changes; all three ABIs for native/build changes |
| Push another reviewed batch to a **ready** PR targeting `main` | Yes, push and PR events | Yes, push and PR builds may both run to verify branch SHA and PR merge result |
| Manually run **Android CI** and select a branch | Yes | ARM64 by default; select `full_abis` to build all three |
| Merge into `main` | Yes (post-merge) | Yes, **all three ABIs** (post-merge) |
| Signed release | Separate manually dispatched release workflow | Separate, requires signing configuration |

A draft PR may still show a **skipped PR-event** GitHub Actions run; however its **branch push** still starts Android CI. Ready PRs continue running Kotlin tests, lint, native ARM64 compilation, and APK verification. The production release and integrated `main` continue building ARM64, ARMv7 and x86-64. **When a PR changes C/C++, JNI, Gradle, the Android toolchain or the CI verifier, the ready PR automatically runs a full-three-ABI GitHub build and APK validation before merging.**

## Work in batches

1. Start a `feature/` or `fix/` branch based on updated `main`. Make related changes in one batch (for example, all dialog surfaces, then the canvas toolbar). Keep unrelated audit/security fixes in their own batch.
2. Inspect affected callers, resources, tests, and compatibility. Run only permitted **source-level** dry runs such as `git diff --check`, resource reference checks, and the Python source validator. Neither a dry run nor a passing static check is an Android build.
3. Commit locally throughout the batch, without pushing each commit. **One reviewed push of the completed batch automatically starts Android CI** on that branch. Additional pushes create additional runs; keep them intentional.
4. Optionally open a **draft** PR; its PR-triggered build is skipped, but **push-triggered branch CI still runs**. Before declaring it ready, review the entire diff, preserve user data formats, add regression tests for nontrivial changes, and record any QA limits.
5. Push the complete batch once (which runs branch CI). Mark the PR **Ready for review** when integration review is needed; this separately tests the PR merge result against `main` with the **Source, resources and attribution** job and **JDK 17 / tests / lint / debug APK** job. If a ready PR changes afterward, GitHub reruns these checks to verify its new head.
6. Branch pushes already produce checkpoint APKs. For a manual recheck or explicit full-ABI run, open **Actions → Android CI → Run workflow**, select the feature branch and run it **after** pushing the batch. The default feature-branch APK targets ARM64. Select **full_abis** to request all three architectures manually; ready PRs already auto-detect native/toolchain changes and build all three. This manual option will only be exposed reliably after the updated workflow is available on GitHub's default branch; switch the repository default from `master` to `main` after reviewing its settings. Record the tested commit SHA. Manual verification of a branch tip does not substitute for required PR merge checks where the PR merge commit differs.
7. Merge to `main` only after all required checks pass on the current PR, plus review of any runtime/device risks. A `main` push triggers one post-merge safety run. Production signing remains a separate manual release gate.

## Rules that prevent false green results

- **No** `continue-on-error` or removed assertions to make a failing build look successful.
- **No** unchecked direct push to `main`; use branch protections requiring the existing source and Android-build statuses. The connector cannot currently verify branch protection because GitHub denies access; confirm it in repository settings.
- **No** Android build from local commits; **every pushed branch batch runs CI**. Do not delete `pull_request.synchronize`: it verifies the ready PR's integration result as well (sometimes causing two runs for one push).
- **No** local Gradle builds/tests. If a test is needed, use GitHub Actions on the selected branch.
- **No** production release merely because unit tests pass; device checks of stylus/palm rejection, dialogs, backup/save, and PDF handling remain separate acceptance requirements.
- **No** treating ARM64-only debug APKs as universal builds; x86-64 emulators and 32-bit ARM devices require a full-ABI checkpoint.
- Release signing and F-Droid builds do **not** pass `ciArm64Only`; the default Gradle configuration remains the original three ABIs.

## Compatibility with existing branches

This policy is effective when the branch being pushed **contains the updated** `.github/workflows/android-ci.yml`. Long-lived branches made before this policy may still have the older push triggers; first reconcile their workflow file from `main` as part of a reviewed batch, because GitHub evaluates the workflow on the pushed ref. Bring those branches up to date with `main` after the policy lands, or cherry-pick the workflow-only change without mixing unrelated features. The repository currently reports `master` as the default branch; switch that separately after reviewing branch rules and release workflows. Manual workflow dispatch remains available because Android CI already exists on the default branch.

The policy deliberately keeps one post-merge Android run on `main` as a safety check until main-branch protection is confirmed. Removing it prematurely could allow direct or unreviewed pushes to escape Android verification.
