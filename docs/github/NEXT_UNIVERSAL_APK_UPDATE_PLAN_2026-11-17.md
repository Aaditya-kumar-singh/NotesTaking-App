# Next Universal APK Update — B100 UI + Pending Feature Integration

**Created:** 2026-11-17

**Status:** Local source integration for B100 UI, NP-004, C regression sources and Chat A's NT-054 Android XML-parse repair candidate; **not yet cloud-built, merged, or device verified**

**Previous test baseline:** B99 `b299cbb8972267eebe103493fa093f7e5cfa6f70`, [GitHub Actions #43857426156](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43857426156). Keep the verified B99 artifact and its real-device smoke results intact.

**UI requirement:** [GitHub #28](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/28), [B100 redesign specification](../engineering/B100_LIBRARY_EDITOR_CHROME_REDESIGN_2026-11-16.md), and the four-page editable brief supplied to the user.

## Next update: required scope and sequence

| Order | Priority | Deliverable | Owner | Gate |
|---|---|---|---|---|
| 1 | P1 | Diagnose misplaced **New Note** pill/tooltip, keep exactly one accessible bottom-right library FAB and selection-mode Export PDF | Coordinator, shared `Backstage.kt` and resources | Runtime reproduction and no duplicate action |
| 2 | P1 | Persistent editor **filename at top**, Back/Home and rename; put split-pane toggle separate from drawing tools; remove bottom filename duplication | Coordinator, `MainActivity.kt`; Chat A supports existing title/rename helpers only after exclusive scope | Narrow-pane and long-name UI tests |
| 3 | P1 | **Chevron up** to show existing Pen/brush variants, Highlighter, Eraser, Lasso, Shapes, Text etc; **chevron down** to hide; active tool remains visible | Chat B on B-owned toolbar components, after exclusive claim and no concurrent Chat B writer | Tool switching, Back/focus, scroll and accessibility checks |
| 3a | P1 / triage | Investigate **selection-bar hit targets**: B99 ADB taps on Open side by side and Clear Selection did not respond; confirm manual touch and fix any overlay/hit-test regression ([issue #29](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/29)) | Coordinator on shared library chrome after reproduction | Two selected QA notes open in panes; Clear Selection responds; keyboard/TalkBack equivalent works |
| 3b | P0 / NT-054 | Saved native `flow.xml` text can disappear from editor and PDF after reopening, in both near-top and lower-page cases ([issue #30](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/30)). Chat A identified unsupported Android parser security features and supplied an XML-recovery repair candidate. | A-owned source repair integrated locally; CI/real-phone diagnosis and byte-level PDF extraction required | Both original QA markers visible/editable after cold restart and present in exported PDF; no native-note data loss |
| 4 | P1 | Integrate Chat A's source-reviewed **NP-004 Quick Scratch storage helper** into the actual UI, with no unsafe duplicate-note writes | Coordinator shared UI + Chat A-owned reviewed helper | Create/open/back/save on disposable notes |
| 5 | P2 / decision | Chat C **NP-102 handwriting recognition**: complete and source-review, or explicitly defer from initial testing build; do not claim recognition without a working model/phone evidence | Chat C, independent existing claim | Explicit inclusion/deferral decision |
| 6 | P1 | Review exact worker SHAs, perform source/privacy/ownership/regression checks, push only a new QA branch under coordinator publish lock | Coordinator | Exact-SHA CI JUnit/lint/universal all-ABI debug APK |
| 7 | P1 | Install the **new** test APK on an authorized phone; compare B99 versus B100 screens and rerun affected NT tests | A/B/C owners + operator/coordinator | Evidence with source SHA, APK SHA-256 and screenshots |
| 8 | Release gate | Final `developer` integration, production signing and distribution only after REL-14/16/19/20 evidence and approval | Coordinator | No unverified protection bypass |

### NP-004 Quick Scratch source checkpoint (2026-11-17)

On local coordinator branch `integrate/np004-quick-scratch-ui-20261117`, the reviewed Chat A Quick Scratch helper (`4895eb2`, cherry-picked with provenance) is integrated with a single **Quick Scratch** row in the existing library More menu. The primary New Note/selection-mode Export PDF FAB is unchanged. The action captures the currently selected SAF root and folder, rejects concurrent taps, creates the native blank note on `Dispatchers.IO`, opens only a non-null created URI, and shows the existing localized create failure otherwise. English and Simplified Chinese labels are present. Existing A-owned helper regression sources are preserved; focused coordinator source assertions cover the UI dispatch contract. **This is not Kotlin/JUnit execution, cloud CI, APK/device verification, a completed NP-004 release gate, or proof that cloud SAF writes always succeed.**

The next accepted APK must exercise both internal storage and selected SAF folders, duplicate `Untitled` names, denied/revoked permissions, rapid duplicate taps, Back, edit/save/cold reopen, and no unintended files. Keep the source-reviewed `NP-004 = local_ready` status until exact integrated-SHA CI and real-device evidence are available.

### B100 toolbar and core QA integration checkpoint (2026-11-18)

Coordinator branch `integrate/b100-toolbar-pdfcache-20261118` preserves the reviewed Chat B toolbar implementation and test-correction sequence, plus Chat C's approved NP-102 handwriting *input-planning only* and source-only rotated-capture, flow-visibility and 241-page cache-churn tests. The top editor header now opts both toolbars into `headerChromeVisible = true`, removing only redundant bottom filename/Home/Close Pane controls. Shared editor keyboard routing sends unmodified keys to the directly focused pane, not to a focused child toolbar or form; Ctrl document shortcuts remain available. A new coordinator-owned pure routing test records the intended decision, and accessible arrow labels are updated in English and Simplified Chinese.

**Evidence level at this B100 checkpoint:** local source integration and Python checks only. Kotlin/JUnit tests, Compose focus and Back/IME behavior, actual PDF cache performance, signed APK, CI and Galaxy M13/tablet QA have not run. The older B99 installed APK contains none of these additions. The later NT-054 checkpoint below includes Chat A's local repair candidate but does not prove P0 resolved; no release gate, NP-102 recognition feature or REL-08 hardware task is verified.

### NT-054 Android XML parser repair candidate (2026-11-18)

Coordinator branch `integrate/nt054-xml-reopen-repair-20261118` now includes provenance-preserving copies of Chat A's exclusive issue #30 diagnostic commit `0665cbbd05f983b165defc4b70b53716c2c3681c` and parser repair commit `5f53f171e379f12eacf329e9bfcdee1053c7dfd1`, on top of the reviewed B100/NP-004/C integration at `a2a5381a7145e112aa61192daf74e03a2d0b3733`. The source-level diagnosis is that Android may reject an unsupported mandatory JAXP feature during `SecureXml.builder()`, so `FlowXml.readInto()` silently leaves the persisted text unparsed. The updated `SecureXml.parse(bytes)` performs strict UTF-8 / BOM-marked UTF-16 decoding and rejects DTD/entity declarations before best-effort parser hardening. `FlowXml`, XFDF and SVG importers use the shared parser; native note schema and saved files have not been rewritten.

**Tests authored, not executed:** `SecureXmlTest` checks ordinary namespaced XML, malformed encodings, UTF-16 and DTD/entity rejection. `NativeFlowVisibilityTest` writes a disposable native archive, reopens identical bytes twice, and checks the saved marker in `FlowFrame`, screen `FlowPainter` and a PDF flow hook using fake renderers, for 2-blank near-top and 48-blank lower-page cases. These assertions do **not** test Android's actual XML parser, Compose rendering or extractable PDF bytes. The lower-page fixture uses a synthetic font/layout and may not capture mobile defaults.

**Next exact-candidate gate:** First run the published candidate SHA through GitHub Kotlin/JUnit, lint and all-ABI debug APK CI. On the authorized Galaxy M13, reopen **copies of the two original B99 QA notes** (one with confirmed active IME), compare their original ZIP/flow hashes, confirm markers are visibly editable in the editor and extractable from a freshly exported PDF, then verify ink, images, audio and page order remain unchanged. Include direct original-fixture/long-paragraph/large-font overflow scenarios; preserve the immutable originals and stop on unexpected data mutation. Issue #30 remains **P0 open** until this evidence passes. No protected branch update, signed release or device claim follows automatically from these local commits.

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
