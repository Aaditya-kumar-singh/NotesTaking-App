# Execution order: Notein findings → independent NotesTaking updates

This is the action order for [FEATURE_MATRIX.md](FEATURE_MATRIX.md) and the user-approved [hybrid product contract](HYBRID_PRODUCT_CONTRACT.md). **Keep all unique NotesTaking features, adopt the best observed reference behavior on overlapping flows, and do not copy Notein's code or assets.** Advance a task only when it is the highest-priority **unblocked** item, its upstream branch/CI checkpoint is known, and the change can be reviewed safely.

## Acceptance and Git discipline

1. Before editing: read `AGENTS.md`, [LOCAL_FIRST_PARALLEL_WORKFLOW](../github/LOCAL_FIRST_PARALLEL_WORKFLOW.md), [BATCHED_CI_POLICY](../github/BATCHED_CI_POLICY.md), and the relevant Notein evidence and NotesTaking callers.
2. Start a small, named local branch from the exact verified/intended ancestor. No direct unreviewed changes on `developer`, `main` or `master`. Do not touch `.kotlin/`.
3. Implement the smallest complete behavior. Update only target source/resources/tests; keep native note formats, annotations and storage backward-compatible.
4. Run **source-only** Python project checker, backup/privacy checker if relevant, resource checks and staged `git diff --check`. Never locally run Gradle, Kotlin tests, lint, native builds, APK installs on Windows.
5. Commit locally, update exact SHA, NP IDs, checks and status in [PROGRESS.md](PROGRESS.md). **Do not push each commit.**
6. When the previous published CI checkpoint is resolved, fast-forward each complete short-lived batch onto `developer` without squash and publish one reviewed development checkpoint. Verify remote exact SHA and GitHub Kotlin/unit/lint/APK/CodeQL where configured. Each batch's original commit remains visible even if several are published together.
7. For any gesture/UI/export/data safety change, complete applicable [TEST_PLAN.md](TEST_PLAN.md) tests on an Android phone **and** S Pen tablet. A source check is not a device pass. Record binary hash, OS/device, test artifacts and observed output.
8. Only then mark requirements **VERIFIED**, merge following repository review policy, and update the matrix to avoid repeatedly implementing existing work.

## Current implementation checkpoint (2026-11-11)

- `developer` commit `62c46b9` contains the unsquashed B08–B12 commits; Android CI run [#42971179169](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/42971179169) passed the exact SHA, including native APK/lint/tests. Physical QA is still pending.
- **B13–B17 integrated:** `developer` now at `edb45f1` and [Android CI #42973906542](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/42973906542) passed the exact SHA, including APK upload. All stylus and visual QA on a physical device remains pending.
- **B18–B22 integrated and CI-verified:** `developer` at `090e860`; [Android CI #43000500557](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43000500557) **PASSED** (including Kotlin tests/lint/APK). B22's symlink fix resolved the prior failure at run #42977549121; B21's null SAF output stream check also passed its tests. Physical QA pending.
- **B23 passed CI:** integrated into `developer` at `808d0b2`, [Android CI #43004628716](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43004628716) passed its Kotlin tests, lint and APK. Real SAF provider acceptance remains pending.
- **B24 CI passed:** `developer` at `2870f59` passed [Android CI #43009071252](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43009071252), including Kotlin tests, lint and APK. Real provider QA pending.
- **B25 CI passed:** `developer` at `3723f93` passed [Android CI #43174700715](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43174700715) (Kotlin tests, lint and APK). Real provider QA pending.
- **B26 CI passed:** `developer` at `35718de` passed [Android CI #43179688881](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43179688881) after the nullable PNG check correction `86a416d`; real phone/tablet acceptance pending.
- **B27 CI passed:** `developer` at `82ccf7d` passed [Android CI #43193305719](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43193305719); physical SAF acceptance still pending.
- **B28 CI passed:** `developer` at `55f4e2c` passed [Android CI #43200542583](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43200542583), including Kotlin tests/lint/APK; provider acceptance pending.
- **B29 CI passed:** `developer` at `d1f8155` passed [Android CI #43205675656](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43205675656), including Kotlin unit tests/lint/APK. Physical malformed PDF and cloud provider QA remain pending.
- **B30 CI passed:** `developer` at `5e7cdd0` passed [Android CI #43209554300](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43209554300) (Kotlin tests, lint, APK). Device acceptance pending.
- **B31 CI passed:** `developer` at `5639c7f` passed [Android CI #43211899857](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43211899857), including Kotlin unit tests, lint and APK; device QA pending.
- **B32 CI passed:** `developer` at `96356f7` passed [Android CI #43214443501](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43214443501), including Kotlin tests/lint/APK; real malformed-PDF QA pending.
- **B33 CI passed:** `developer` at `9858f44` passed [Android CI #43242272480](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43242272480), including Kotlin tests/lint/APK; physical provider QA pending.
- **B34 CI passed:** `developer` at `8a404c3` passed [Android CI #43245830585](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43245830585) for PDF insertion validation and cleanup, including Kotlin tests, lint and APK. Real device QA pending.
- **B35 CI passed:** `developer` at `fb049bc` passed [Android CI #43249908551](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43249908551) including Kotlin tests/lint/APK; large-PDF device memory QA pending.
- **B36 CI passed:** `developer` at `3ca3b6e` passed [Android CI #43253245086](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43253245086), including Kotlin tests/lint/APK. Real-device QA pending.
- **B37 CI passed:** `developer` at `b68613f` passed [Android CI #43258248682](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43258248682) for native editable page extraction, including Kotlin tests/lint/APK; physical QA pending.
- **B38 CI passed:** `developer` at `b2e38f3` passed [Android CI #43263675874](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43263675874) including Kotlin tests/lint/APK for native Extract/Merge early cancel.
- **Parallel coordination CI passed:** two-GPT LocalMCP setup at `developer` `71c494d` passed [CI #43395813192](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43395813192); worktree ownership and hooks source-verified, no physical QA.
- **B39 CI passed:** `developer` at `e6b73d8` passed [CI #43397349803](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43397349803) including Kotlin tests/lint/APK for zero-byte atomic save protection. Force-stop and low-storage device acceptance pending.
- **B40 CI passed:** `developer` at `47625dd` passed [CI #43399003215](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43399003215) including Kotlin tests/lint/APK for persistent paged/infinite/PDF toolbar selected-state semantics (Chat B `8f8c877`); physical TalkBack validation pending.
- **B41 CI passed:** `developer` at `fd72e00` passed [CI #43400721628](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43400721628) including Kotlin tests/lint/APK for bulk native-note asset saving. Save-time hardware measurement pending.
- **B42 reviewed integration `8cf608a` (Chat A `203eadc`):** SAF provider copies use existing `BoundedCopy` stalled-read protection and preserve cleanup on failure; tests cover repeated zero-byte reads and intermittent empty reads with 128 KB byte identity. Source/privacy checks pass; GitHub CI and real provider QA pending.
- **Master replacement is future work**, after all in-scope backlog items are implemented and non-device verification passes; preserve master rollback and release workflow, no blind force-push. See [HYBRID_PRODUCT_CONTRACT.md](HYBRID_PRODUCT_CONTRACT.md).

## Other remote branches not yet reconciled into developer (2026-11-11)

Git reports remote branches outside `developer` ancestry. **Do not assume they are all missing features:** release branches, old fixes, Dependabot upgrades and historical branch structures are mixed together. After B08–B12's combined development CI, review the latest `origin/main`/`origin/master` code differences and bring over **only still-missing, safe changes** as their own reviewed batch(es). The most relevant known candidates are:

- Tablet PDF finger handwriting and area-eraser repaint: own-code fixes `65d67a6` and `2235d9f` are reconciled into the B14/B15 local candidate (`a568090`/`32ab31c`). These repairs remain hardware-QA pending.
- Reliability and native import/clipboard/Trash improvements: the first-party fixes and regression tests from `origin/fix/audit-reliability-batch-2026-10-30` have been inspected and cherry-picked into the B18–B20 candidate on top of `developer`. Native release/CI workflow commits remain excluded. Reconcile any *additional* unique `main`/`master` changes separately.
- Main → master promotion/release workflows and some native/ABI CI changes: already in current `origin/main`/`origin/master`; **do not** blindly merge release automation into a development workflow.
- Dependabot/library updates and old feature branches: audit separately; count neither an unmerged Git ref nor a shared SHA as evidence of a user-visible missing feature.

Do not mark the 110 feature requirements complete from branch counts or a merge alone.

## Ordered work packages

| Batch | Priority | NP scope | Implementation and concrete definition of done | Dependencies |
|---|---|---|---|---|
| **G00 Baseline** | P0 | NP-001–NP-110 | Freeze exact `developer` baseline, B08/B09/B10 branch SHA, Android CI and device QA states. Reconcile predecessor batches without accidental history rewrites. | Existing branches; GitHub access |
| **G01 Honest Export UI (B11)** | P1 | **NP-073** | Explain shareable PDF vs still-editable native note in existing export dialog; clarify visual PDF annotation behavior in EN/zh-Hans; source/translation checks. No fake format option. | Can be completed locally on B10; no rendering-engine rewrite |
| **G02 Tablet Ink & Eraser Reliability** | P0 | NP-020–NP-029, NP-039–NP-048 | Reproduce stylus/palm, pointer ownership, B09 scrollbar fix, area/stroke/selection modes, eraser cursor/pressure and flicker; add targeted deterministic tests; record real S Pen evidence. | B08/B09 Kotlin CI, physical S Pen tablet, disposable PDF |
| **G03 Save/Restore Security** | P0 | NP-071, NP-081–NP-085, NP-087 | Review bounded ZIP, path traversal, untrusted file limits, Save As/SAF retry, old-copy survival, app kill/low storage; fix only reproduced gaps; verify disposable file hashes. **B12** hardens settings-backup ZIP import locally at `a8647c5`; CI/hardware tests pending. | Disposable test notes/URI provider; CI fixtures |
| **G04 PDF Coordinate/Performance Gate** | P0 | NP-053–NP-056, NP-061–NP-070 | Verify rotation/crop/margins, zoom, PDF text/annotations, 200-page memory, thumbnail/viewport behavior; fix confirmed mapping/cache gaps. | Real PDF fixtures + tablet performance evidence |
| **G05 Pen/Eraser Depth** | P1 | NP-030–NP-038, NP-040–NP-052 | Finish pen preset B10 CI/device QA; assess eraser pressure, marker-only/tape-only erasing, selection erase, layer constraints and overdraw; preserve undo and strokes. | G02 baseline and correct layer semantics |
| **G06 Pages & Native Save Page As** | P1 | NP-057–NP-063, NP-065, NP-066, NP-085–NP-087 | Validate B06/B07 page manager and export, build independent new editable note from chosen pages if absent, copy assets transactionally, show correct restore/feedback. | G03 safety and G04 page coordinates |
| **G07 Export Capability Extensions** | P1/P2 | NP-072–NP-080 | After G01 labeling, design/add truly supported PDF output variants, JPEG, background/annotation/stroke switches, margins, optional watermark. Each option gets an actual renderer+MIME+SAF implementation and output-file regression. | Output contract research; G03/G04 |
| **G08 Library & UX Accessibility** | P1/P2 | NP-001–NP-019, NP-089–NP-100 | Finish Quick Scratch, note-level favorites/tags, grouped toolbar settings, onboarding/help, 48dp targets and TalkBack, phone/tablet responsive layout. Preserve existing Explorer semantics. | Existing B01–B07 UI regression checks |
| **G09 PDF Layers/Study** | P2 | NP-050–NP-051, NP-059–NP-069 | Research/implement true named-layer UI where the current model supports it, annotations list, selection tools, multi-page study workflows, independent Save Page As and deep page navigation. | File compatibility and performance tests |
| **G10 Offline OCR and Handwriting Research** | P2/P3 | NP-101, NP-102, NP-105 | Only after security/privacy/model license checks: prototype offline printed OCR, bounding boxes and searchable index, English/Hindi handwriting recognition. No automatic remote upload. | Model size/accuracy/ABI and battery benchmarks |
| **G11 Optional Account/Cloud/AI** | P3 | NP-088, NP-103–NP-104, NP-106–NP-108 | **Explicit product decision required** before writing code: cost, privacy, credentials, encryption, consent, and offline fallback. Not part of mandatory guest note-taking. | Requirements/authorization |
| **G12 Release Acceptance** | P0 | All implemented NP IDs | Verify phone & tablet at supported themes/font scales, PDF/ZIP/old-note roundtrip, source and native APK, rollback policy. Close NP items one by one; record installer links and hashes only when published. | Each package verified by exact SHA |

## Known committed work that must not be done again

- B01–B07 toolbar, floating toolbar, favorites, library/home, new note, PDF page panel and export dialog are **already** on developer ancestry.
- **B08** `feature/pdf-reading-annotation-ux-b08` at `5633d01` was pushed; Android CI run #42962656473 passed on the exact SHA. Tablet stylus and PDF UI/device acceptance remains pending.
- **B09** `feature/stylus-eraser-scrollbar-b09` at `5072725` was **published** after B08 CI passed. [B09 CI #42966793225](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/42966793225) started; real tablet acceptance is pending. This is not the full palm/eraser solution.
- **B10** `feature/saved-pen-styles-b10` at `65d1aaa` is committed **locally**; three actual presets per stroke tool, shared by paged/infinite editors. Kotlin/device tests pending.
- **B11** `feature/notein-parity-master-plan-b11` locally committed at `29daa41`, followed by ledger update `a50bdf2` (Notein tracker plus honest shareable PDF copy). **B12** `fix/settings-backup-zip-budget-b12` locally committed at `a8647c5` on top of B11, tightening settings archive import. Do not treat B09/B10/B11/B12 as CI-passed because B08 is green.

## What not to build just because the reference APK contains it

Notein's branded artwork, database schema, proprietary stroke algorithms, login/paywall, cloud infrastructure and AI service calls are not requirements for source copying. Implement a behavior only with NotesTaking-owned code, explicit user value, stable note formats, real provider integration and verified offline/privacy behavior. `GAP` and `OPTIONAL` items stay visibly open until their exact acceptance tests pass.
