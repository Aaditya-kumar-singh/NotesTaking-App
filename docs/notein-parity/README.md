# Notein → NotesTaking App: master implementation tracker

**Created:** 2026-11-11
**App being updated:** NotesTaking App (native Kotlin + Jetpack Compose), package `com.notestaking.app`.
**Reference:** `Notein_Official.apk` v1.2.607.0, package `com.orion.notein.global`, SHA-256 `3a5b703f06174117741e3c4b9f27ecabefe7d02e9be65084c344e67a372353f6`.
**Read-only reference evidence:** `E:\Notein_ReverseEngineering\` (11 dated first-party investigation reports, APK/REA method records and phone screenshots).
**Comparison base:** local NotesTaking App feature branch descended from B08–B10 at `65d1aaa`. This is **not** a claim that every feature has been compiled or tested.

## Read this folder in order

1. [HYBRID_PRODUCT_CONTRACT.md](HYBRID_PRODUCT_CONTRACT.md) — preserve our features, improve overlapping behavior toward Notein's observed UX, and plan a controlled future master replacement.
2. [PERFORMANCE_ACCEPTANCE.md](PERFORMANCE_ACCEPTANCE.md) — measurable frame/input/eraser quality goals, with hardware verification still pending.
3. [SOURCE_EVIDENCE.md](SOURCE_EVIDENCE.md) — what was actually observed in the APK, and what remains uncertain.
4. [FEATURE_MATRIX.md](FEATURE_MATRIX.md) — comprehensive, prioritized feature-by-feature comparison and concrete acceptance conditions. This is the **source of truth** for remaining work; every item has an NP ID.
5. [BATCHES.md](BATCHES.md) — safe implementation order, dependencies, source touchpoints, and branch/CI gates.
6. [TEST_PLAN.md](TEST_PLAN.md) — reproducible test cases for phone, real S Pen tablet, PDFs, undo, exports, recovery and accessibility.
7. [PROGRESS.md](PROGRESS.md) — exact commits, CI state, and change log. Update after **every** local batch/publish/merge.

## Truth labels used by the tracker

| Label | Meaning | Can we mark the feature done? |
|---|---|---|
| **SOURCE** | Already has a corresponding NotesTaking source path. Device/edge-case success **not** established. | No |
| **PARTIAL** | Some required behavior or UI exists; remaining gap is named. | No |
| **LOCAL** | A scoped change was committed on a local branch, but not verified by GitHub Kotlin CI or hardware. | No |
| **PUBLISHED** | Exact commit pushed to GitHub; CI state tracked separately. | No |
| **GAP** | Needed capability is not evidenced in NotesTaking source or is explicitly still research. | No |
| **INVESTIGATE** | Evidence is not sufficient to call it implemented or missing. | No |
| **OPTIONAL** | Accounts/AI/cloud or commercial services need an explicit privacy, architecture and licensing decision; they do not block offline note taking. | No |
| **VERIFIED** | Exact branch SHA passed Android CI **and** applicable device + file-roundtrip acceptance cases passed with recorded evidence. | Yes |

**Never convert SOURCE, LOCAL, PUBLISHED or a passing source-only Python check into VERIFIED.** Not all Notein XML layouts proved live functionality. Notein's exact UI/permissions were sampled on a **Samsung SM-M135F phone**, not a stylus tablet, and no real Notein export or restore round-trip was executed.

## Implementation boundaries

- Work through [BATCHES.md](BATCHES.md), starting with the highest-priority **unblocked** item. Make the smallest complete independently designed update; cite the NP IDs in the batch commit/report. **Use `developer` as the rolling integration branch:** implement/commit each finished batch on a short-lived branch, then preserve those commits when fast-forwarding into `developer` at the next reviewed CI checkpoint. Do not squash or push every intermediate commit.
- No reuse of Notein's proprietary code, reverse-engineered implementations, artwork, fonts, database schema, branding or credentials. Reimplement behaviors independently using the existing NotesTaking stack and original assets.
- Keep NotesTaking **offline-first**, usable without an account. Do not add mandatory sign-in, network sync, analytics, paid subscriptions or remote AI calls. Optional modules stay behind an explicit user/product decision.
- Preserve existing `.xnote`, `.xcanvas`, `.xtemplate`, source PDFs, existing settings, file permissions and prior stored notes. Never silently convert a native note to a PDF. Avoid destructive migration.
- Follow [Local-first workflow](../github/LOCAL_FIRST_PARALLEL_WORKFLOW.md) and [Batched CI policy](../github/BATCHED_CI_POLICY.md). On this Windows machine **do not run Gradle, Kotlin tests, Android lint, NDK/CMake, APK builds, or device installs**. Run source-only checks and commit local checkpoints; GitHub CI and disposable-data device QA provide acceptance.
- Keep the pre-existing untracked `.kotlin/` directory untouched. Do not overwrite B08's pushed SHA or merge B09/B10 before exact-commit validation.
- Safety/privacy fixes outrank visual parity; source-verified safe import/ZIP/SAF logic should **not** be replaced with Notein's code (which had unresolved security/durability questions).
- The historical `docs/audit/xnotes-problems-master.xlsx` register is separate and remains open until individually evidenced. Matrix NP items are **not** its closure record.

## Current implementation starting point

B01–B17 are integrated into `developer` at `edb45f1`. GitHub [Android CI #42973906542](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/42973906542) passed on that exact commit, including Kotlin tests, lint, APK validation and artifact upload. B18–B20 were integrated into `developer` at `eb29851`, but CI #42977549121 failed one symlink-creation regression test out of 2,505. B22 `f2611cc` corrects that test's underlying filename collision path; B21 `26ae143` fixes null output streams during shared native note creation. Both are local commits pending a combined developer CI rerun and device tests. The app follows [HYBRID_PRODUCT_CONTRACT.md](HYBRID_PRODUCT_CONTRACT.md): keep NotesTaking-only features, improve shared behavior against observed Notein UX, and defer controlled `master` promotion until all agreed in-scope changes are ready. See [PROGRESS.md](PROGRESS.md) for current batch status.
