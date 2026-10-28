# NotesTaking App — Whole-App Status, Risks and Master Action Plan

**Verified on:** 2026-10-28 (Asia/Kolkata)
**Project:** `C:\Users\WHITE-SNAKE\Desktop\xnotes-android`
**GitHub:** https://github.com/Aaditya-kumar-singh/NotesTaking-App
**Current tracked branch:** `master`; latest checked commit `facce2cacb7404478f92d6961df131d6c1765bd7`.
**Status:** SOURCE/CI TRIAGE + PRIORITIZED ROADMAP — **not** a full UI device audit, and not a certification of safe/releasable code.
**Scope:** Kotlin/Compose app, native C/C++ PDFium/Tree-sitter/GL, saved notes, PDF, audio, pen/eraser, explorer, settings, theme, import/export, CI/release, security, compatibility and planned enhancements.

## 1. Verified health dashboard

| Area | Grounded result | Decision |
|---|---|---|
| Git repository | Public `Aaditya-kumar-singh/NotesTaking-App`, default `master`, remote configured, HEAD `facce2c`. | Established; update stale setup docs. |
| GitHub Android CI | Workflow run [40685349818](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/40685349818), success, 2026-10-28 on committed HEAD. Workflow builds tests, lint, debug APK and package check. | **Committed baseline builds.** Does not cover uncommitted canvas changes. |
| Beta release | [v0.1.0-beta.1](https://github.com/Aaditya-kumar-singh/NotesTaking-App/releases/tag/v0.1.0-beta.1) published; workflow [40685357309](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/40685357309) succeeded; asset is `NotesTakingApp-v0.1.0-beta.1-debug.apk` plus SHA-256. | Debug-signed testing build, **not** production-signed or Google Play-ready. |
| CodeQL | Latest seen Java/Kotlin scan on commit `126c073` passed; native C/C++ requires separate review. | No blanket security clearance. |
| GitHub tracking | GitHub showed zero open issues and zero open PRs at check; five Dependabot branches remain; master reports **protected:false**. | Create prioritized tickets/branch protection after decisions, review dependencies individually. |
| Working tree | Modified `ui/Editor.kt`, `ui/Toolbar.kt`, `ui/InfiniteToolbar.kt`, English and Chinese `strings.xml`; new untracked `ui/CanvasQuickAccess.kt` and `docs/canvas/`. | New canvas features **not committed/pushed/tested in CI**. Preserve before any further merges. |
| Local structural checks | `python tools/ci/check_project.py` — 361 production Kotlin, 197 test Kotlin, 19 XML, 39 PNG, **0 structural errors**. Rebranding verifier passed; `git diff --check` clean. | Not a Kotlin compile or runtime pass. |
| Local Android toolchain | Java executable unavailable; `JAVA_HOME` unset; `ANDROID_HOME` unset; no `app/build/outputs/apk/debug/app-debug.apk`. | Configure JDK 17/SDK/NDK/CMake or rely on GitHub CI after safely committing changes. |
| Device/instrumentation tests | No `app/src/androidTest` tree. No current device screenshot or stylus/input/instrumentation results reviewed. | Add instrumentation tests and real tablet testing. |
| Prior audit | `docs/audit/findings.md`: 45 **historical static findings**; `docs/audit/remaining-work.md`: 96 proposed validation scenarios, 0 Android cases recorded at that checkpoint. Original hashes are against pre-rebrand source and need re-baselining. | Treat as triage queue. Multiple high-risk patterns were **reconfirmed** in today's code; not all 45 were individually revalidated. |
| App compatibility | min SDK 26, compile/target SDK 36, versionName 0.1.0, versionCode 1, three Android ABIs; JDK17, pinned NDK27/CMake3.22.1. | Test API26–36, phone/tablet/fold/split and native ABI loading. |

## 2. What already exists vs what remains

| App surface | Existing source-backed capability | Remaining required work | Priority |
|---|---|---|---|
| Home/explorer | Notes/folders, library organization, previews, Trash, rename/share operations in `Backstage.kt`, `Explorer*`, `TrashPane.kt`. | Root SAF storage safety, folder atomic copy, actual delete outcome, modern empty/loading/error states, permissions tests. | P0/P1 |
| Paged notebooks | Freehand strokes, multiple pens, ruler/shapes, page styles, text, images, selection, history in `ui/Editor.kt`, `core/model/`. | Save integrity, pressure/gesture quality, eraser geometry, ergonomics and device performance. | P0/P1 |
| PDF reading/annotation | PDFium, PDF text selection/markup, PDF original + separate ink, PDF bookmarks/search and export; single/double/cover viewing modes exist. | Large/corrupt PDF limits and cancellation, PDF/paper mix export roundtrip, easier PDF UI, page insertion before/after/end with template workflow. | P0/P1 |
| Infinite canvas | Separate infinite editor, GL renderer, shapes/ink, waypoints/minimap, zoom, canvas export. | Restore/split data safety, eraser parity, GPU allocation/teardown tests and responsive toolbar. | P0/P1 |
| Canvas tool discoverability | Existing customizable horizontal/vertical toolbar; local uncommitted `CanvasQuickAccess.kt` adds Eraser, +Page and All Tools entry points. | **Still need pinned Pen/Highlighter/Lasso/Undo/Redo**, grouped searchable All Tools, color/width preview, real on-device toolbar QA. | P1 |
| Eraser | Both STROKE/AREA modes and size slider are in `Popups.kt`. | Fix `Stroke.kt:694+` sample-center clipping: straight two-point lines currently erased entirely; continuous swept erase and undo/zoom parity. | P1 |
| Pen/highlighter | Pen/dashed/calligraphy/speed/taper/highlighter, pressure/width, color swatches. | One-tap user favorites, previews and accurate width across zoom, gesture defaults and hover ring. | P1 |
| Text and markdown | `FlowEditor`, `MarkdownParser`, tables, typing and syntax highlight support. | Text editing/selection correctness, keyboard/IME/TalkBack, restore and formatting test matrix. | P1 |
| Audio | Recording service, audio chips/player, attachment and audio data models. | Microphone permission/background/lifecycle/notification tests, denied permission, partial recording cleanup, output validity. | P1 |
| Templates/styles | Paper colors/sizes, packaged templates, stickers/images, settings preferences. | Enforce limits on `.xtemplate` extraction, import cleanup, complete UI previews. | P0/P2 |
| Search and navigation | Page thumbs, contents, bookmarks, text/search index. | Clear on-canvas entry points and result/page transform checks, OCR integration only for scans. | P1/P2 |
| Settings, appearances | System/light/dark/OLED Material color, corner and toolbar settings, backup/restore. | Privacy backup policy, bounded imported settings, preserve user theme; UI brand theme + alert/popup consistency. | P0/P2 |
| Launcher/logo/splash | Existing first-party branded adaptive icon, splash and loader assets. | Original final vector/logo, launcher variants, refreshed branded theme and visual QA. | P2 |
| OCR | **Not present as an integrated OCR engine**; PDFium already extracts selectable PDF text. | Choose/test printed OCR English/Hindi (fully offline/privacy), scan indexing; stylus handwriting recognition separate feasibility. | P2/R&D |
| DOCX/EPUB | **No native DOCX/EPUB import/editing verified**. MIME handling covers PDF/Markdown. | Define safe document conversion/import architecture, preserve formatting, explicit limitations; don't advertise editable DOCX prematurely. | P2/R&D |
| Offline/privacy | Primarily local-document app; Android manifest does not request INTERNET; storage through Android SAF/own provider. | Determine auto-backup policy, protect URIs/temporary data, test restore/deletion, document privacy. | P0 |
| Native subsystem | Bundled PDFium / Tree-sitter / GL renderer, native JNI for supported ABIs. Historical host Tree-sitter smoke 9,005 parse cases passed. | Exact vendor version/CVE mapping, fuzz/ASAN/UBSAN where practicable, huge file/perf/ABI tests. | P1 |
| GitHub automation | Actions for CI, beta, signed releases, CodeQL and Dependabot exist. Committed baseline CI is green. | New branch checks for local changes, review Dependabot, branch protection and tickets, final signed release environment/secrets. | P1 |
| Documentation | Comprehensive `docs/audit`, 120 `CV` planned/enhance tasks, rebrand and design docs. | Remove stale instructions saying GitHub was never connected, link this roadmap, rebase historical findings to new source paths, consolidate duplicate tickets. | P1 |

## 3. Release blockers: verified source-risk patterns (P0)

The historical XN IDs are from the pre-rebrand static audit. **Today's source inspection reconfirmed these specific patterns**, but no malicious-file exploit, actual data loss or device failure was reproduced.

| Risk cluster | Current evidence | Fix and exact proof |
|---|---|---|
| Session data destruction or stale save | `platform/SessionStore.kt:51-70` deletes prior destination on failed rename and swallows errors; `CanvasSessionStore.kt:30-47` and `JsonStore.kt:20-31` are analogous. `ui/Editor.kt:2913-2932` records `lastSessionContentVersion` before async success. | Replace with checked, durable staged writes using atomic replacement + recovery copy; serialize writers by pane; publish success only after persistence; process kill/failing-stream/race tests. XN-004,010,022,023. |
| Save/close guard failure | `MainActivity.kt:604-612` allows canvas-open branch to skip dirty guard; old audit XN-001,015,024,025. | Save result propagated, no false success, explicit Save As/Discard/Cancel, keep note open if write fails, SAF provider fault injection. |
| Split pane data loss | `ui/Editor.kt:2914` refuses to save before `sessionLoaded`; historical source trace XN-037 and XN-038 found second-pane restore and link-navigation bypass issues. | Initialize/restore both pane slots, guard every file/open action, keep session per pane and recovery fixtures; process-death tests. |
| DocumentsProvider traversal/root mutation | `platform/AppStorageDocumentsProvider.kt:72-103` delete/rename/copy/move root without explicit protection; `fileFor` accepts relative docId and creates File without checked canonical containment; recursive `copyTree` is unbounded. | Central root/descendant validation, input name/type validation, block destructive root operations and copying into self, staged copy with rollback, Android SAF negative instrumentation. XN-005,012,030,031. |
| Dangerous/unbounded imports | `format/DocumentCodec.kt:753-790` streams archive assets without global inflated budget and `flowBytes=zis.readBytes()`; `format/FlowXml.kt:319-323` uses default XML parser; `format/TemplatePackage.kt:57-65` extracts image without inflated cap. | Hard byte/entry/depth budgets and XML entity/DTD hardening, cleanup on abort; hostile ZIP/XXE/giant PDF/image tests. XN-002,003,014,017,026,027,028,029. |
| Clipboard/image memory and PDF staging | `platform/SystemClipboard.kt:53`, multiple `MainActivity.kt` and `ui/Editor.kt` unbounded `readBytes` cases. | Bounded streaming, decode downsampling/size checks, off-thread import and memory-pressure tests. XN-008,018,020,029,040. |
| Backup privacy | `AndroidManifest.xml:27` sets `allowBackup=true` with no explicit extraction/backup filters; app-private sessions/attachments exist. | Decide which actual notes merit backup vs what must stay out; add dataExtractionRules/fullBackupContent and test backup/restore; document behavior. XN-032. |

## 4. Functional quality issues (P1)

- **Stylus/multi-pointer:** historical XN-033 found stylus pointer-up/finger fallback; XN-034 found mixed-contact four-finger debug cancellation. Confirm current pointer tracking, then add synthetic MotionEvent tests in paged and infinite controllers; run physical stylus and palm tests.
- **Area eraser:** current `core/model/Stroke.kt:694-721` still performs sample-center splitting; `straight` returns whole-stroke deletion on any contact. Need true segment/capsule clipping, historical samples, continuous drag, stable undo/redo, roundtrip output tests. Refer `docs/canvas/SAMSUNG_NOTES_STYLE_ERASER_PLAN.md`.
- **Undo:** `core/history/History.kt:37-47` removes commands before invoking them. A thrown undo/redo loses stack entry; add exception-safe stack transitions and injected failures (XN-036).
- **PDF import/export:** test merged source pages, inserted blank sheets, output page count, annotations, links/bookmarks/rotation, long document cancel and PDF original preservation.
- **Rendering:** inspect old XN-042 to XN-044 (texture retries, template padding and potential blocking GL teardown), benchmark fast ink and high DPI, orientation churn and GPU memory.
- **Trash, audio, text and shares:** error reporting, offline/permission behaviour, temporary-file cleanup and accessibility should be part of feature-level acceptance, not assumptions.

## 5. UX, branding and feature completion (P1/P2)

- **Canvas layout**: finish current quick-access pass: pinned Pen, Marker, Eraser, Select/Lasso, Undo/Redo, readable state, Page N/M, color and width; all tools category/search, simple/advanced preset, visible +Page. Phones/tablets, left-hand, floating/vertical rail and split pane. Current local shortcuts still need Kotlin compile and screenshot/device review.
- **Notebook & PDF**: +Page Before/After/End with paper type and PDF source insertion, thumbnails, bookmarks/search, clear distinction freehand marker vs PDF text highlight, consistent view and page jump. Reuse existing engines.
- **Themes/app brand**: original final editable icon master, launcher round/monochrome, splash/loading, Material color tokens, light/dark/OLED, typography, icons, dialogs, undo snackbars, errors, destructive confirmation. Do not change PDF paper/ink due to UI theme.
- **Optional research before shipping**: offline OCR for printed text (English + Hindi), stylus handwriting recognition (different problem), DOCX/EPUB conversion, advanced pencil/brush textures, Easy Writing Pad. These are **not** complete app capabilities today.

## 6. Implementation sequence / release gates

| Order | Phase | Exact deliverable | Gate |
|---|---|---|---|
| 0 | Secure baseline and build the local patch | Preserve current uncommitted files; create isolated feature branch, JDK17 + SDK36 + NDK27 + CMake3.22.1, run compiler/unit/lint/APK; or commit safely to branch and use GitHub CI. | New patch compile + tests + lint pass. |
| 1 | Prevent data loss | Fix session atomicity, save results/guard and both-pane restore/open navigation, reliable undo. | Failure injection + process-death save/reopen tests pass. |
| 2 | Storage/import defenses | Document-provider validation, root guard, ZIP limits, XML hardened parser, image/clipboard/PDF size caps, cleanup; explicit backup/privacy policy. | Malicious file and Android SAF instrumentation pass. |
| 3 | Input correctness | Rebuild precision eraser, paged/infinite parity, palm/hover/side-button, 2-point strokes, undo/export. | Stylus device suite and the 18 eraser acceptance scenarios in the eraser plan; no ink/data loss. |
| 4 | Canvas and PDF UX | Finish pinned toolbars, context strip, All Tools with search, +Page templates and PDF merge, Page N/M/Search/Bookmarks/View; screenshot QA. | User can discover key tools within <=2 taps; PDF mixed-page save/reopen/export. |
| 5 | App-wide UI and workflow | Home/explorer, create note, trash, recording, settings, prompts, notifications, icon/splash/theme; accessibility and localization. | Complete E2E tasks on phone/tablet, talkback/font scale. |
| 6 | Performance/native/versioning | Vendor security advisory mapping, PDFium/Tree-sitter/GL tests, huge PDF and stress, profile low-memory, supported ABIs/API26-36. | No blocking crashes/performance regressions; dependencies/licenses documented. |
| 7 | Optional capabilities | Prototype OCR then index/search/export; investigate DOCX conversion/EPUB, handwriting recognition and advanced brushes separately. | Ship only independently benchmarked working features with privacy guarantees. |
| 8 | Production release | Consolidate 45 source findings into GitHub issues (triage and close only with evidence), protect master and require CI, configure 4 signing secrets, bump version+tag, build signed artifact, audit it and publish after manual acceptance. | Signed release validated; debug beta is not substituted. |

## 7. Device acceptance matrix

- **Devices:** Android API 26/28/33/36, phone 320–400dp widths, 8–11 inch tablet portrait/landscape, Samsung/other stylus if available, low RAM device, keyboard/mouse, TalkBack 200% text.
- **Data:** create/rename/delete/restore, autosave, no-storage and full-storage errors, process death (1 pane + 2 panes), SAF provider revoked, file provider errors, ordinary .xnote/.xcanvas/.xtemplate fixtures from original XNotes.
- **PDF:** searchable PDF, scanned PDF (no OCR promise), large/rotated/linked PDF, blank-page insert between imported pages, page reorder, annotations, export/reopen.
- **Handwriting:** pen types/pressure, palm rejection, finger pan, stylus side eraser, fast eraser sweep, straight line partial erase, zoom/rotation, undo and redo.
- **Other:** audio recording permission denied/granted/background, image import huge/invalid, markdown/table edit, custom fonts/templates, theme changes, Back/gesture navigation and split resize.

## 8. GitHub and documentation actions

1. GitHub HEAD `facce2c` CI and beta release are already green; **do not use old docs claiming GitHub hasn't been initialized**. Refresh `README.md`, `docs/github/WORKFLOWS.md`, `docs/github/VALIDATION_STATUS.md`, and `docs/rebranding/REBRANDING_STATUS.md` as historical vs present tense (do not rewrite historical evidence).
2. `master` is currently not protected per branch API; set an appropriate PR/ruleset and required checks once available; avoid changing branches to hide working-tree modifications.
3. GitHub has no open issues/PRs at check; create focused issue categories rather than opening 120 duplicated design tickets at once. The prior 45 historical findings are source triage, not 45 demonstrated exploits.
4. Reconcile `docs/canvas/FEATURE_BACKLOG.md` (CV-001–CV-120, with overlapping parent/child tickets) and `docs/audit/xnotes-problems-master.xlsx`; distinguish existing, in-progress-local, committed and device-verified.
5. Keep original MIT XNotes notice and native dependency licenses. Review all GitHub Dependabot upgrade branches against AGP/Kotlin native compatibility; do not merge failing PRs blindly.
6. The local `docs/canvas/` directory and `CanvasQuickAccess.kt` are currently untracked; they will be excluded from push unless explicitly added. Confirm no private notes, temporary artifacts, compiled keys or cached test images are staged.

## 9. Immediate next 10 tickets (suggested issue order)

1. **APP-P0-001:** Preserve working tree, compile quick-access patch in GitHub feature branch, obtain screenshots/APK.
2. **APP-P0-002:** Recoverable session writes and acknowledged saves in `SessionStore/CanvasSessionStore/JsonStore`.
3. **APP-P0-003:** Save fail/close guard; preserve user work and support Save As / Retry.
4. **APP-P0-004:** Secondary pane restore and guard all open/navigation paths.
5. **APP-P0-005:** DocumentsProvider root/path/recursive-copy safeguards and instrumentation.
6. **APP-P0-006:** ZIP/XML/template/PDF import budgets and malformed-file tests.
7. **APP-P1-007:** Undo failure handling and robust history tests.
8. **APP-P1-008:** Stylus/mixed-touch regressions and true vector partial erase.
9. **APP-P1-009:** Finish canvas toolbar and +Page workflow with screenshots.
10. **APP-P1-010:** Test/regression matrix, security/privacy audit, new signed release gates.

### What was not done in this report
No Android app ran on a device; no new Gradle build of uncommitted code, instrumentation test, source bug fix, signature/key setup, pull request, commit, push, GitHub issue or release was created. The current audit's 45 historical risks need individual revalidation after patching; mere source-line retrieval isn't semantic or runtime evidence.

### Evidence references
- Latest successful CI: https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/40685349818
- Published testing APK: https://github.com/Aaditya-kumar-singh/NotesTaking-App/releases/tag/v0.1.0-beta.1
- GitHub latest beta run: https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/40685357309
- Local `docs/audit/findings.md`, `remaining-work.md`, `progress.md`.
- Local `docs/canvas/CANVAS_FIRST_PASS_IMPLEMENTATION_STATUS.md`, `CANVAS_DISCOVERABILITY_AND_WORKSPACE_PLAN.md`, `FEATURE_BACKLOG.md`.
- Current Android source paths documented above and `python tools/ci/check_project.py` output.
