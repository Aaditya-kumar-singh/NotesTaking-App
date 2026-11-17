# NotesTaking — consolidated bug, feature and delivery backlog

**Snapshot:** 2026-11-17 (Asia/Kolkata)  
**Repository:** [Aaditya-kumar-singh/NotesTaking-App](https://github.com/Aaditya-kumar-singh/NotesTaking-App)  
**Basis:** existing GitHub open issues #18, #20, #23–#30; `docs/engineering/B100_LIBRARY_EDITOR_CHROME_REDESIGN_2026-11-16.md`; `docs/notein-parity/TEST_PLAN.md`; existing Galaxy M13 QA reports; live local `triad.py status`.  
**Purpose:** one *planning view*, **not** new NP/REL cards, issue duplicates, permission to modify source, release sign-off, or an assertion that unverified features are done.

## 1. What is really outstanding?

Local coordinator ledger at capture: **130 task cards total** = **119 local_ready**, **10 pending**, **1 in review (NP-102/C)**, **0 active**, **0 finally verified**, **0 device_verified**, **0 deferred**. Local readiness means source/review preparation, not accepted CI + physical device QA. **0/26 A–Z batches closed.** GitHub's open issue search returned 10 issue trackers; they are not one-to-one with the 130 cards.

**Baseline under physical QA:** B99 universal debug APK, exact source `b299cbb8972267eebe103493fa093f7e5cfa6f70`; Samsung SM-M135F Android 14, `com.notestaking.app.debug`. This is **not** proof that latest agent source changes are in the installed APK. No source implementation of B100 UI redesign is established.

### A. Fix-first bugs and investigation

| Priority | Tracking | Problem and evidence | Next bounded action / completion gate |
|---|---|---|---|
| **P0** | [#30](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/30), NT-054 | After confirmed IME input, text exists in saved `flow.xml` and survives cold restart, but is invisible in editor and absent from PDF. Near-top and lower-page QA fixtures support this. | Exclusive A/coordinator source investigation of flow layout, page cursor anchors, draw invalidation and PDF text painting; regression at top/bottom, note cold restart, and exported PDF; exact integrated APK phone retest. **Not fixed.** |
| **P0 release gate** | [#18](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/18), NT-060/061/063 | SAF/output failure and atomic native save/rollback remain partially verified. Ordinary cancellation, occupied-name and complete-session restart tests passed but cannot prove interrupted staged writes. | Isolated, disposable fault-injection harness; provider 17 empty reads, transient 0-byte reads, bad metadata, sidecar failures, low storage, cancellation during 200-page render/final copy; compare last-good hashes and cleanup. Never corrupt the personal phone's live session. |
| **P1 investigation** | [#29](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/29) | Library selection's Split and Clear ignored injected taps in B99; human finger reproduction remains absent. | Manual touch + Mobile Tester human mode, screen hierarchy and overlay hit-test; inspect `Backstage.kt` / `ExplorerChrome.kt` with coordinator. If reproducible, one shared-file fix + acceptance after B100 changes. |
| **P1** | [#23](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/23) | Rotated screenshot and selection-image alignment may diverge from live page transformations. | A-only transform/crop source and golden comparisons at 0/90/180/270, margins, PDF and ink; only implement with exclusive scope, then CI/device check. |
| **P1** | [#24](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/24) | Tooltip positions/width might be based on full window instead of narrow pane; TalkBack and keyboard not fully validated. | B-owned pane/window matrix at 180/240/320dp, 1.3x/2x text and Hindi labels; prove defect before smallest tooltip change; exact-source tests and device QA. |
| **P1** | [#20](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/20) | Historical audit findings: GL decode/teardown, PDF temp/limits, session latency, SAF negative paths, SVG limits, split ratio. Some source fixes proposed or reviewed, not individually device-verified. | Reconcile XN-006/009/020/027/028/029/039/040/042/044 etc. against exact integrated source and test evidence; leave XN-011 DOCX import as product decision, not a bug. |

### B. B100 user-requested UI / product design — issue #28

**Source:** [B100 UX issue #28](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/28) and [complete existing spec](B100_LIBRARY_EDITOR_CHROME_REDESIGN_2026-11-16.md). All rows are **planned, not implemented or phone-tested**.

| ID | UI change | Minimum correct design and acceptance |
|---|---|---|
| **UX-01** | Relocate **New Note** | Diagnose purple top-left pill: actual `CreateButton` is already BottomEnd in source, so suspect tooltip/anchor before adding a new FAB. Keep **one** bottom-right New Note FAB unobstructed above IME/nav bar; preserve selection Export PDF and New Canvas/Import PDF overflow. |
| **UX-02** | Filename/title at top | Persistent app bar: Back/Home, active-pane filename with ellipsis/full-title accessibility, page count/type and Rename; split at top-right. Remove duplicate truncated bottom filename. Preserve unsaved prompt and pane focus. |
| **UX-03** | Arrow to expand tools | Collapsed 48dp chevron-up plus selected-tool state; expanded chevron-down and existing Pen, Highlighter, Eraser, Lasso, Select, Pan, Shape, Text, Text box, brush variations and supported image/audio actions. Close on selection on phone; Back collapses drawer first. |
| **UX-04** | Pencil request | **Not yet an independently verified brush feature.** If there is no distinct existing Pencil brush, leave Pen unchanged; separate scope to design a real Pencil mode after review. Never fake it by renaming Pen. |
| **UX-05** | Split / page controls | Split and page manager stay outside drawing tools; independent per-pane title/drawer/active-tool state; no canvas recreation or ink mutation. Retest #29 selection actions. |
| **UX-06** | Accessibility / responsive | Minimum 48x48dp targets, 8dp separation; Hindi/English, TalkBack, Tab/Enter/Space, 180/240/320dp split, 320/360dp phone, 1x/1.3x/2x fonts, IME, landscape, dark/OLED, reduced motion. |
| **UX-07** | Regression / CI | B100-01 through B100-14 in existing spec: drawing, undo/redo, saved notes, native ZIP hashes, PDF/export correctness, full-ABI universal debug build on exact SHA and Galaxy M13 screenshots vs B99. |

### C. Already listed feature and coverage tracks

| Priority | Tracker | Work still needed |
|---|---|---|
| P1 | [#25](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/25) NP-004 | Quick Scratch one-tap creation and native save correctness; A-owned implementation and coordinator-only shared button wiring, then actual B100 phone test. Do not duplicate New Note UI work. |
| P1 | [#26](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/26) REL-12 | TalkBack/keyboard/split-pane and toolbar acceptance on new exact-SHA APK (not only source tests). |
| P1/P0 hardware | [#27](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/27) NP-102, REL-08–11 | C's NP-102 source-planning **in review**; English/Hindi real handwriting recognition not built or verified. Stylus pressure/palm, eraser, PDF geometry and 200-page performance need proper tablet/fixtures. |
| P1 pending QA | NT-055 / [#18](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/18) | Five-page nonadjacent `1,3-5` PDF/PNG export matrix: synthetic five-page QA source created and pushed to phone; import blocked by tool permissions, not an established app defect. Resume only on verified QA note import. |
| P1 pending QA | NT-057 / #18 | Compatible external PDF annotation editor must actually select/modify individual Ink objects; Drive PDF Viewer navigation alone is not editability. |
| P1 pending QA | NT-026 / #18 | Read/Annotate/Eraser/Read/Annotate state restoration was being tested in B99; **session incomplete**. Prior Read toggle observation is a partial step, not signed off. |
| P2 | NT-058/059 | Actual JPEG quality/filename checks where implemented; PDF export option combinations. Source `JpegExportPlan` alone is not shipped phone UI proof. |
| P0 release | [#18](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/18) | Exact integrated-SHA CI, real Android hardware coverage including S Pen tablet, controlled failure cases, production signing secrets and independently verified signed release; debug APK alone is not a stable release. |

## 2. Ordered execution — do not mix feature changes with open P0 data-loss candidates

**Phase 0: Preserve QA and authorize scope.** Freeze B99 test artifact and existing synthetic fixture hashes; verify coordinator and exact worktrees. Coordinator approves B100 design and one owner per shared file. GitHub issue #28 already exists: **do not create duplicate issue or fake NP task**.

**Phase 1: P0 correctness / data integrity.** A + coordinator diagnose and patch #30. In parallel create a safe provider/staged-write test harness for NT-060/061/063 and prepare repeatable `QA_*` fixtures. Exact-source regression and new debug APK are required before claiming fix.

**Phase 2: P1 confirmed interaction bugs.** Manually reproduce #29; if real, coordinator fixes shared library selection/overlay hit testing. A completes #23 if the transform error is demonstrated; B completes #24 if pane tooltip clipping is demonstrated. Do not patch assumptions.

**Phase 3: B100 UI implementation, exclusive ownership.**
1. **Coordinator:** shared `Backstage.kt` single FAB/tooltip anchor, `MainActivity.kt` shared editor header, strings/localization and feature integration; coordinate #29 before shipping.
2. **Chat B:** toolbar drawer and existing tool registry reuse inside B-owned Toolbar/ToolbarFrame/FloatingToolbar/InfiniteToolbar/Tooltip components, plus UI tests, **only after an explicit exclusive supplementary claim**.
3. **Chat A:** A-owned `Editor.kt` title/pane-state helpers and tests if individually assigned. Preserve NP-004 ownership; shared Quick Scratch entry belongs to coordinator.
4. **Chat C:** NP-102 review stays isolated; only read-only geometry feedback for B100 unless a new exclusive scope is recorded.

**Phase 4: Integrate and validate.** Coordinator reviews worker SHAs, ownership guard, Python source/privacy checks, exact integrated-SHA GitHub CI + all-ABI universal debug APK. Device QA repeats B100-01..14, NT-054, NT-026, NT-055/056/057, NT-060/063 and split/tooltip/keyboard/TalkBack matrix. Compare B99 versus B100 captures.

**Phase 5: Release gating.** Resolve selected P0/P1 scope, stylus-tablet checks, production signing and rollback. Publish only under coordinator lock. P2/P3 optional deferral requires explicit recorded decision.

## 3. Concrete ready-to-assign work packets

| Packet | Owner suggested | Claim/gate before source edit | Deliverable |
|---|---|---|---|
| B100-ROOT | Coordinator | Exclusive shared-file scope/issue #28 approval | Investigate New Note pill anchor + selection overlay; patch only proven root |
| B100-TOP | Coordinator + A | Coordinator grants exact A Editor helper paths | Top filename and split/rename semantics, keyboard/split regression |
| B100-TOOLS | Chat B | Exclusive B supplementary issue claim (avoid #24 overlap) | Accessible arrow drawer using existing tools, view states and tests |
| B99-TEXT | Chat A + coordinator | Explicit P0 #30 repair ownership, check any live A claims | flow.xml text rendered/reopened/exported, with regression |
| B99-SELECT | Coordinator | Finger repro + #29 evidence | Selection Split/Clear touch handling and focus |
| B99-GEOMETRY | Chat A / C read-only | #23 approval; no C source without ownership | Render-to-export rotation match at 0/90/180/270 |
| B99-TOOLTIPS | Chat B | #24 exclusive scope | Pane-safe popovers / TalkBack + keyboard verification |
| QA-RELEASE | QA operator / coordinator | Correct latest debug APK, disposable data | Exact-SHA QA and negative-path report, no source code |
| C-NP102 | Chat C | Existing NP-102 is already in **review** | Complete authorized review; no new claimant or fake recognizer done state |

## 4. Guardrails and definition of done

- **Do not edit** `developer`, `main`, `master`, agent worktrees or protected release states in this planning pass. A local planning commit is not a pushed/merged implementation.
- Preserve existing unrelated untracked files and Mobile Tester reports. At audit time, phone was not connected to LocalMCP; an NT-026 test session had been running. **Do not infer its clean completion from previous screenshots.** Resume/close safely when the device reconnects.
- Each card needs: testable expected result, affected paths/owners, regression tests, exact commit SHA + CI result, physical phone/tablet evidence when required, acceptance and any explicit untested conditions.
- Never claim the New Note FAB, top filename, expandable tools or Pencil redesign implemented until source change and device tests actually confirm them.
- The user can resume hardware testing later with the Mobile Tester skill. The next development decision is **coordinator authorization for B100 shared-file scopes**, with #30 text reliability handled ahead of release.
