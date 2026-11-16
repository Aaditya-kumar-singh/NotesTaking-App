# REL-17 — main/master reconciliation and rollback anchors

**Audit date:** 2026-11-16
**Disposition:** Local, source-only coordinator reconciliation. **No merge, remote branch movement, release tag, or signing approval.**

## Pinned heads

| Branch | SHA | Role |
|---|---|---|
| `developer` | `807c7221145d1b09c6e521be5676a9da9100e7de` | Active November 2026 integration baseline |
| `main` | `19de06dbc6e69622aa4a17872773d4d8c2090c57` | Stable branch with October 30 release automation |
| `master` | `13fbb60b605049f8864c365da1b26f774181dd4e` | Older default/release branch |
| `qa/b93-c-reviewed-20261116` | `7baaa966ed255f291aad5ae55fe41adb3420dd8d` | Source-reviewed C-only QA checkpoint; CI result still unconfirmed |

These are the exact `git ls-remote` branch heads at the audit. Refresh the remote state immediately before any future integration.

## Relationship and unique changes

- `master` is an ancestor of `main`. `main` has **11 commits** not on `master`; `master` has **zero** commits not on `main`. Do not duplicate or separately merge `master` into `main`.
- `developer` and `main` diverged after `1ebf44212517a36dcd3b3f01308a1495bb155316`. `developer` has **427** commits absent from `main`; `main` has **21** absent from `developer`. `developer` and `master` have 427 and 10 respectively.
- `git log --no-merges --cherry-pick --right-only origin/developer...origin/main` leaves **five patch-unique commits** needing targeted review rather than an unconditional replay:
  - `65d67a6`: enable finger handwriting from tablet PDF toolbar. `developer` already exposes finger-inking controls and relevant input handling, but full behavior equivalence needs code-level integration review and real tablet acceptance.
  - `1520df0`: automated verified-`main` promotion, versioned `master` beta and `auto_beta.py` helpers. This was designed for an older release model and must **not** be enabled on the active `developer` flow without an explicit policy decision.
  - `d3c8dc8` and `d5c5016`: actionlint, ShellCheck and checksum checks in the historical automated release workflows. Reconcile only with the chosen release workflow.
  - `23fcd6a`: Beta 4 release workflow parameter adjustments. Historical release behavior, not an authorization to deploy a new release.
- Older common-tree fixes on both histories touch the native codecs, clipboard/image cleanup, GL retry behavior, resource translations and tests. Patch equivalence across merged commits is **not** proof that these behaviors pass on the current Android build.

## Read-only merge simulation

`git merge-tree --write-tree origin/developer origin/main` reports **seven conflicting paths**:

`.github/workflows/codeql.yml`, `InteractionController.kt`, `CanvasQuickAccess.kt`, `FreePointerTest.kt`, `ImportCleanupTest.kt`, `SafeDocumentTreeTest.kt`, `SessionStoreTest.kt`.

A hypothetical direct merge of `master` into `developer` reports **five conflicts** (interaction controller, canvas quick access, free-pointer test, safe-document-tree test, session test).

**Decision:** Do not merge either whole release branch into `developer`. Preserve active November drawing/storage behavior and history. For any older fix missing in `developer`, identify the exact caller and test, cherry-pick or port only that verified change into a separate coordinator integration branch, then validate on the exact combined SHA. Keep automated promotion and signing independently reviewed.

## Local rollback refs created and verified

| Local reference | Pinned SHA |
|---|---|
| `refs/backup/notes-20261116/pre-integration-developer` | `807c7221145d1b09c6e521be5676a9da9100e7de` |
| `refs/backup/notes-20261116/pre-integration-main` | `19de06dbc6e69622aa4a17872773d4d8c2090c57` |
| `refs/backup/notes-20261116/pre-integration-master` | `13fbb60b605049f8864c365da1b26f774181dd4e` |
| `refs/backup/notes-20261116/qa-b93-c-reviewed` | `7baaa966ed255f291aad5ae55fe41adb3420dd8d` |

Created with `git update-ref` using a zero-old-value compare to avoid overwriting an existing reference; each verified with `git rev-parse`. **These anchors are local only.** They are not remotely accessible or sufficient alone for disaster recovery. Before promotion or release, the coordinator must create and verify an authorized remote rollback reference under the publishing lock, subject to branch-protection and retention policy.

## Release-stage gates still open

1. Preserve the A/B/C and Gemini handoffs without rewriting ancestor commits. Review the pending NP-015, NP-107 and NP-076 results and integrate only approved changes.
2. Obtain an exact-SHA GitHub Actions result for the QA/integration candidate (source, Kotlin/JUnit, lint, **all three ABIs**, APK integrity). A local Python check is not an Android build.
3. Collect physical-device storage/SAF recovery, tablet stylus/palm/ink, PDF 200-page/memory, accessibility and permission-denial evidence. No required device card is verified.
4. Reconcile the chosen release tag/version, signing-key continuity, explicit approval of promotion policy, remote rollback anchors and signed APK/hash validation before moving `main`, `master` or publishing a release.

**Status:** REL-17 branch graph and rollback-anchor preparation complete for source review only; merge/release acceptance remains blocked.
