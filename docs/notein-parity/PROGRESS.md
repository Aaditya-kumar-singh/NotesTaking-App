# Implementation / CI evidence ledger

**Updated:** 2026-11-11. All changes refer to NotesTaking App, NOT the Notein APK. This ledger is the source of truth for evidence levels: local commit ≠ published ≠ CI green ≠ hardware acceptance.

| Batch | NP coverage | Branch / exact short SHA | Source state | GitHub Android CI | Real device / export acceptance |
|---|---|---|---|---|---|
| B01–B07 | NP-009–010, NP-033–035, NP-057–060, NP-072, NP-089–090 (partial) | `developer` `ac0508d` | Implemented and pushed | Prior Android CI passed | **Pending** tablet/phone QA |
| B08 | NP-029, NP-066 | `feature/pdf-reading-annotation-ux-b08` `5633d01` | Pushed | **Passed** full Android CI on exact SHA; [run #42962656473](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/42962656473), completed 2026-11-11 | Pending S Pen tablet/device QA |
| B09 | NP-022 | `feature/stylus-eraser-scrollbar-b09` `5072725` | **Pushed**, remote SHA verified; source checks passed | [CI #42966793225](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/42966793225) **in progress** at 2026-11-11 checkpoint | Pending S Pen/finger/scrollbar device QA |
| B10 | NP-032 | `feature/saved-pen-styles-b10` `65d1aaa` | Locally committed; source checks passed | **Not run** for B10 | Pending settings roundtrip/gesture |
| **B11 / G01** | NP-073 (PDF/native export clarity) | `feature/notein-parity-master-plan-b11` `29daa41`, ledger `a50bdf2` | **Committed locally**; 110 NP/79 NT IDs, resource and privacy checks passed | **Not run** for B11 | Pending phone/TalkBack export copy review |
| **B12 / G03** | NP-087, NP-083 (ZIP safety support) | `fix/settings-backup-zip-budget-b12` `a8647c5` | **Committed locally**; strict ZIP reader, skipped-member 64 MiB accounting, new streaming regression tests; source/privacy checks passed | **Not run** for B12 | Pending malformed settings backups and settings-roundtrip verification |
| G02–G12 | Remaining NP tasks, refer to [BATCHES.md](BATCHES.md) | Not started | Planned / needs evidence | Not run | Pending |

## Evidence collected this turn

- Located and re-read 11 previous Notein reverse-engineering phase reports in `E:\Notein_ReverseEngineering\`; confirmed source APK SHA-256 matches exact published report.
- Checked current NotesTaking source, previous B01–B10 engineering documents, local branches and standing `AGENTS.md`/CI rules.
- Created the 110-item [FEATURE_MATRIX.md](FEATURE_MATRIX.md), evidence mapping, ordered work packages and 79 acceptance tests in this folder. Committed the tracker plus EN/zh-Hans export copy correction as `29daa41` on local B11.
- Published the separately reviewed B09 `5072725` after B08 CI completed successfully; verified GitHub remote SHA and [CI run #42966793225](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/42966793225) started. This does not establish a passing B09 build.
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
