# Next Universal APK Update — B100 UI + Pending Feature Integration

**Created:** 2026-11-17

**Status:** Scheduled in next update plan; **not yet implemented, merged or verified**

**Previous test baseline:** B99 `b299cbb8972267eebe103493fa093f7e5cfa6f70`, [GitHub Actions #43857426156](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43857426156). Keep the verified B99 artifact and its real-device smoke results intact.

**UI requirement:** [GitHub #28](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/28), [B100 redesign specification](../engineering/B100_LIBRARY_EDITOR_CHROME_REDESIGN_2026-11-16.md), and the four-page editable brief supplied to the user.

## Next update: required scope and sequence

| Order | Priority | Deliverable | Owner | Gate |
|---|---|---|---|---|
| 1 | P1 | Diagnose misplaced **New Note** pill/tooltip, keep exactly one accessible bottom-right library FAB and selection-mode Export PDF | Coordinator, shared `Backstage.kt` and resources | Runtime reproduction and no duplicate action |
| 2 | P1 | Persistent editor **filename at top**, Back/Home and rename; put split-pane toggle separate from drawing tools; remove bottom filename duplication | Coordinator, `MainActivity.kt`; Chat A supports existing title/rename helpers only after exclusive scope | Narrow-pane and long-name UI tests |
| 3 | P1 | **Chevron up** to show existing Pen/brush variants, Highlighter, Eraser, Lasso, Shapes, Text etc; **chevron down** to hide; active tool remains visible | Chat B on B-owned toolbar components, after exclusive claim and no concurrent Chat B writer | Tool switching, Back/focus, scroll and accessibility checks |
| 3a | P1 / triage | Investigate **selection-bar hit targets**: B99 ADB taps on Open side by side and Clear Selection did not respond; confirm manual touch and fix any overlay/hit-test regression ([issue #29](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/29)) | Coordinator on shared library chrome after reproduction | Two selected QA notes open in panes; Clear Selection responds; keyboard/TalkBack equivalent works |
| 3b | P0 / NT-054 | Triage saved-but-invisible **flow text** when placed low on an A4 note; the native text survives but reopens invisibly and one-page PDF export omits it ([issue #30](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/30)). Confirm a near-top input control with active caret/IME first | Coordinator allocates exclusive A/C/shared flow-render scope after reproduction | Lower-page and top-page text visible, editable after cold restart, and present in PDF export; no invisible overflow |
| 4 | P1 | Integrate Chat A's source-reviewed **NP-004 Quick Scratch storage helper** into the actual UI, with no unsafe duplicate-note writes | Coordinator shared UI + Chat A-owned reviewed helper | Create/open/back/save on disposable notes |
| 5 | P2 / decision | Chat C **NP-102 handwriting recognition**: complete and source-review, or explicitly defer from initial testing build; do not claim recognition without a working model/phone evidence | Chat C, independent existing claim | Explicit inclusion/deferral decision |
| 6 | P1 | Review exact worker SHAs, perform source/privacy/ownership/regression checks, push only a new QA branch under coordinator publish lock | Coordinator | Exact-SHA CI JUnit/lint/universal all-ABI debug APK |
| 7 | P1 | Install the **new** test APK on an authorized phone; compare B99 versus B100 screens and rerun affected NT tests | A/B/C owners + operator/coordinator | Evidence with source SHA, APK SHA-256 and screenshots |
| 8 | Release gate | Final `developer` integration, production signing and distribution only after REL-14/16/19/20 evidence and approval | Coordinator | No unverified protection bypass |

## Required UI acceptance for next APK

- [ ] Library My library/menu/search/filters are never obscured by the New Note tooltip, including after scroll and resume.
- [ ] One bottom-right New Note action remains reachable, and selected-file Export PDF still works.
- [ ] Filename in top bar shows selected document/pane, truncates safely, and offers full-name/Rename access.
- [ ] Split icon stays visible with drawing palette collapsed and follows focused pane.
- [ ] Library multi-select Split and Clear actions respond to touch in both normal and selection states; issue #29 has a documented manual-touch result and an exact-SHA regression pass.
- [ ] Expanded palette contains all **currently implemented** tools; requested **Pencil** remains a separate feature unless a real pencil preset is present.
- [ ] Chevron state changes, native Back dismisses drawer, TalkBack label and 48dp targets are correct.
- [ ] Split panes (180/240/320dp), large fonts (1.3x/2x), phone/landscape/tablet, light/dark and PDF/Canvas layouts have no overlap.
- [ ] Existing ink, undo, autosave, note restart recovery, PDF export/import/annotation and search still pass.
- [ ] NT-054 flow-text recovery and PDF text rendering pass a device retest against issue #30, including an active-IME near-top control and a bottom-page text placement case.
- [ ] GitHub Android all-ABI CI succeeds for exact candidate SHA and new artifact is independently identified on-device.

## Baseline testing before B100 builds

Continue **B99** phone testing on the Samsung SM-M135F with only `QA_*` disposable notes:
1. PDF zoom (fit/25/100/200/400 as supported) and page navigation, checking saved annotation alignment.
2. PDF rotation/crop only when an explicit non-destructive or reversible control is available; stop rather than modify the original exported PDF.
3. Split panes and toolbar hit-testing in portrait and landscape, especially long filename and bottom-row crowding.
4. Capture observed PASS/FAIL/BLOCKED/INCONCLUSIVE; keep baseline failures distinct from not-yet-implemented B100 requests.

Source of truth for phone QA: [UNIVERSAL_APK_PHONE_QA_2026-11-16.md](UNIVERSAL_APK_PHONE_QA_2026-11-16.md), [TEST_PLAN.md](../notein-parity/TEST_PLAN.md); device findings under `docs/notein-parity/device-evidence/`.

**Exclusions:** This plan does not authorize rewriting native note formats, running Gradle or installing builds on the Windows source machine, editing another agent's worktree, or pushing unreviewed code to `developer`, `main`, or `master`. The current B99 APK contains **no B100 UI fixes** and should not be judged as a failed B100 implementation.
