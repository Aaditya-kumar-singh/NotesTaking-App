# Implementation / CI evidence ledger

**Updated:** 2026-11-11. All changes refer to NotesTaking App, NOT the Notein APK. This ledger is the source of truth for evidence levels: local commit ≠ published ≠ CI green ≠ hardware acceptance.

| Batch | NP coverage | Branch / exact short SHA | Source state | GitHub Android CI | Real device / export acceptance |
|---|---|---|---|---|---|
| B01–B07 | NP-009–010, NP-033–035, NP-057–060, NP-072, NP-089–090 (partial) | `developer` `ac0508d` | Implemented and pushed | Prior Android CI passed | **Pending** tablet/phone QA |
| B08 | NP-029, NP-066 | `feature/pdf-reading-annotation-ux-b08` `5633d01` | Pushed | **Passed** full Android CI on exact SHA; [run #42962656473](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/42962656473), completed 2026-11-11 | Pending S Pen tablet/device QA |
| B09 | NP-022 | `feature/stylus-eraser-scrollbar-b09` `5072725` | **Pushed**, remote SHA verified; source checks passed | **Passed** on exact SHA [CI #42966793225](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/42966793225) 2026-11-11 | Pending S Pen/finger/scrollbar device QA |
| B10 | NP-032 | `feature/saved-pen-styles-b10` `65d1aaa` | Integrated into `developer` `62c46b9`, source checks passed | **Passed combined developer CI #42971179169** | Pending settings roundtrip/gesture |
| **B11 / G01** | NP-073 (PDF/native export clarity) | `feature/notein-parity-master-plan-b11` `29daa41`, ledger `a50bdf2` | Integrated into `developer` `62c46b9`, 110 NP/79 NT IDs checked | **Passed combined developer CI #42971179169** | Pending phone/TalkBack export copy review |
| **B12 / G03** | NP-087, NP-083 (ZIP safety support) | `fix/settings-backup-zip-budget-b12` `a8647c5` | Integrated into `developer` `62c46b9`, strict ZIP and overflow guards | **Passed combined developer CI #42971179169** | Pending malformed settings backups and settings-roundtrip verification |
| B13 | Hybrid UX/product contract | `9b2dd17` | Product/UX/performance and final master promotion gates written; local source check passed | New combined CI pending | No performance measurements |
| B14 | NP-026 tablet finger writing | `a568090` | Existing own-code fix reconciled: active finger-writing option without removing PDF Read/Annotate; local checks passed | New combined CI pending | Real pen/finger tablet QA pending |
| B15 | NP-047 eraser dirty-region optimization | `32ab31c` | Existing own-code eraser repaint bound integrated, with unit regression; local checks passed | New combined CI pending | Dense ink visual flicker QA pending |
| B16 | NP-026 PDF finger control UX | `e133b56` | Bilingual accessible mode status and overflow fallback; local checks passed | New combined CI pending | Small screen/TalkBack QA pending |
| G03–G12 | Remaining NP tasks, refer to [BATCHES.md](BATCHES.md) | Not completed | Backlog / needs evidence | Pending | Pending |

## Hybrid product decision and latest integrated baseline (2026-11-11)

- User directs hybridization: retain all useful NotesTaking-only features; for shared features independently improve logic, UI/UX, ergonomics and smoothness toward observed Notein behavior without copying proprietary code or assets. [Contract](HYBRID_PRODUCT_CONTRACT.md); [performance acceptance](PERFORMANCE_ACCEPTANCE.md).
- All eight B08–B12 development commits were fast-forwarded without squash and pushed to `developer` at exact SHA `62c46b9572ed1b34c3409b88f7cc697545c4e096`. [CI #42971179169](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/42971179169) passed, including Kotlin tests, lint and APK. No physical device connected; all hardware-dependent acceptance remains pending.
- B13–B16 local checkpoints: hybrid product contract `9b2dd17`, tablet PDF finger input `a568090` from our earlier `65d67a6`, eraser dirty-region repair `32ab31c` from our earlier `2235d9f`, and bilingual accessible finger mode `e133b56`. All source/backup-privacy checks passed; these code changes are not yet Android-CI verified or device tested at this ledger checkpoint.
- Future intent: only after all agreed source/CI work is complete (hardware QA may remain pending), safely reconcile and replace master application source with developer, safeguarding prior master and release automation; never silently overwrite branch history. No master change now.

## Evidence collected this turn

- Located and re-read 11 previous Notein reverse-engineering phase reports in `E:\Notein_ReverseEngineering\`; confirmed source APK SHA-256 matches exact published report.
- Checked current NotesTaking source, previous B01–B10 engineering documents, local branches and standing `AGENTS.md`/CI rules.
- Created the 110-item [FEATURE_MATRIX.md](FEATURE_MATRIX.md), evidence mapping, ordered work packages and 79 acceptance tests in this folder. Committed the tracker plus EN/zh-Hans export copy correction as `29daa41` on local B11.
- Published the separately reviewed B09 `5072725` after B08 CI completed successfully; verified GitHub remote SHA and [CI run #42966793225](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/42966793225) completed **successfully**, including Android tests/lint/APK. Real S Pen tablet QA remains open.
- 2026-11-11 integration review: `developer` at `ac0508d`, with seven subsequent B08–B12 commits in an unbroken fast-forward lineage; source/backup-privacy checks passed and the tracked tree was clean. `origin/main`/`origin/master` have additional tablet PDF, eraser flicker, reliability, CI and release changes that have **not yet** been reconciled into developer. This is separate follow-on work, not a completed merge.
- Scope decisions: use prior source as behavioral evidence only, no proprietary code/assets; keep note files and privacy intact; no mandatory accounts/remote AI.
- Confirmed settings-backup ZIP input overlooked inflation from ignored future-version ZIP members and did not share native ZIP's duplicate/traversal enforcement; B12 `a8647c5` applies bounded read and streaming discard with a unit regression. No local Kotlin test or app install was performed.
- The existing `docs/audit/xnotes-problems-master.xlsx` historical audit remains separate and **open**; this tracker does not close any of its findings.

## Add one entry after every implementation or verification event

```text
Date / NP IDs / exact branch and commit SHA / files changed
Source-only checks and failures, Kotlin CI link/status for exact SHA
Device and output-file tests performed, with evidence paths
Feature state transitions (SOURCE/PARTIAL/LOCAL/PUBLISHED/VERIFIED)
Known limitations, dependencies and next highest-priority unblocked item
```

Do not record a future CI, test, merge, or automatic follow-up as completed. The Windows host remains build-free under repository policy; `.kotlin/` is untouched.
