# Notein reference → NotesTaking App: complete feature checklist

**Created:** 2026-11-11. Reference: Notein 1.2.607.0; see [SOURCE_EVIDENCE.md](SOURCE_EVIDENCE.md). Current state is **source-based** unless a separate exact-SHA CI/device result is recorded in [PROGRESS.md](PROGRESS.md).

The **reference behavior** is what was seen in Notein APK code, resources, or limited phone walkthrough—not a promise of full runtime functionality. Each task is independent NotesTaking work; never copy Notein code/assets. **A SOURCE label does not mean tested or accepted.**

Legend: **SOURCE** source exists, QA pending; **PARTIAL** incomplete; **LOCAL** local commit only; **PUBLISHED** pushed, exact CI/device pending; **GAP** not evidenced/known missing; **INVESTIGATE** inspect before coding; **OPTIONAL** requires product/privacy decision. All items remain open until their listed acceptance criteria pass.

## Library, onboarding, creation and organization

Evidence and NotesTaking targets: N29, N31, N32, N33 · NotesTaking `ui/Backstage.kt`, `ui/NewNoteScreen.kt`, `ui/ExplorerChrome.kt`, `ui/TrashPane.kt`.

| ID | Priority | Reference feature / requirement | State | Next implementation or audit | Acceptance gate |
|---|---|---|---|---|---|
| NP-001 | P1 | No mandatory sign-in for core note creation | SOURCE | Preserve guest/offline access; never copy Notein's account gate | Create and reopen an offline note without network/account |
| NP-002 | P2 | Optional short onboarding with Skip | INVESTIGATE | Audit actual first-run flow; add dismissible guidance only if needed | Skip is visible, survives restart, no consent automatically granted |
| NP-003 | P1 | Fast launch into recent/home library | SOURCE | Validate home state persistence and fast first editable stroke | Reopen last note or create one in fewer than three actions |
| NP-004 | P1 | Quick Scratch new blank note affordance | GAP | Add one-tap quick-note creation using existing new-note flow | Tap opens writable note; back saves/asks safely |
| NP-005 | P1 | Standard Note and Infinite Canvas creation | SOURCE | Keep both clear in creation sheet | Both create valid documents in portrait and landscape |
| NP-006 | P1 | Create folders and rename/move notes | SOURCE | Validate nested operations and permission failures | Move/rename no duplicate/lost content |
| NP-007 | P2 | Import documents directly from Create menu | SOURCE | Expose PDF + supported types, warn about unsupported formats | Import valid PDF/notes; fail cleanly on unsupported DOCX |
| NP-008 | P2 | Import image/media into editor | SOURCE | Verify SAF URI limits and foreground result | Image/audio embedded and survives reopen |
| NP-009 | P1 | Paper/cover template preview | SOURCE | B05 added preview; verify real phone/tablet layout | Preview equals created cover/paper appearance |
| NP-010 | P2 | A4, custom sizes and orientation | SOURCE | Verify mm settings, portrait/landscape and PDF mixes | Custom page dimensions persist across reopen/export |
| NP-011 | P2 | Ruled, dotted, grid and blank template choices | SOURCE | Audit template catalog, two-column layouts, accessibility labels | All available templates render and persist consistently |
| NP-012 | P2 | Notebook title and safe naming | SOURCE | Validate reserved names, collisions and cancel | Named note opens without overwriting another |
| NP-013 | P1 | Search library by title/type | SOURCE | Check empty recovery, clear filter, keyboard search | Exact/partial search yields correct note |
| NP-014 | P2 | Sort and grid/gallery/list toggles | SOURCE | Preserve sorting and view choice per folder | Correct order and no lost selection after toggle |
| NP-015 | P2 | Favorites/stars for notes (not favorite pens) | INVESTIGATE | Confirm note-level flag/store; implement only if absent | Starred filter updates and persists across relaunch |
| NP-016 | P2 | Labels/tags and color filters | PARTIAL | Color filters present; label/tag model needs independent design | Tag add/remove/filter with safe migration and multi-note handling |
| NP-017 | P1 | Recent items and Trash recovery | PARTIAL | B19 restores truthful success/failure/partial-delete messages and busy state; real Trash behavior still requires provider QA | Restore note without lost attachments; delete is deliberate |
| NP-018 | P2 | Bulk copy/move/export/delete in library | SOURCE | Verify selection actions and partial failures | N selected documents processed exactly once with errors |
| NP-019 | P1 | Accessible create menu and empty-library recovery | SOURCE | Validate 48dp controls, no dead-end empty screens | Create/Import accessible at phone/tablet sizes |

## Pen input, gestures and general editor tools

Evidence and NotesTaking targets: N13, N21, N26, N29, N32, N33 · `canvas/InteractionController.kt`, `canvas/PenContactRouter.kt`, `ui/Toolbar.kt`, `ui/CanvasQuickAccess.kt`, `ui/InfiniteEditor.kt`.

| ID | Priority | Reference feature / requirement | State | Next implementation or audit | Acceptance gate |
|---|---|---|---|---|---|
| NP-020 | P0 | Stylus takes exclusive ink ownership | SOURCE | Verify full pen-down→up route with pen, palm, second finger | No stroke broken by incidental touch or pinch |
| NP-021 | P0 | Finger-first then stylus-first mixed pointer handoff | SOURCE | Review pointer-ID history and cancel cases across canvases | Stylus starts/finishes normally with resting finger |
| NP-022 | P0 | Stylus/eraser tip bypasses page scrollbar drag | PUBLISHED | B09 `5072725` pushed; CI [#42966793225](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/42966793225) running, tablet QA pending | Pen and eraser draw beside scrollbar; finger scrolling still works |
| NP-023 | P0 | Pressure-sensitive pen strokes | SOURCE | Profile pressure values and low/high-pressure continuity | Pressure affects resulting stroke at 25–400% zoom |
| NP-024 | P2 | Tilt/orientation brush response | INVESTIGATE | Confirm raw axis handling before adding tilt curve UI | Supported tablet responds; unsupported device degrades safely |
| NP-025 | P1 | Smooth in-progress ink with commit on pen-up | SOURCE | Test front buffer cancellation/flush and no double frame | No disappearing ink, doubling or late commit |
| NP-026 | P1 | Stylus-only ink vs finger draw setting | LOCAL | B14 finger-write toggle + B16 state/overflow UX integrated locally; verify cloud build and real pen/finger gestures | Finger pans in stylus-only mode; can draw when enabled |
| NP-027 | P1 | Two/three finger gestures and pinch zoom | SOURCE | Verify no synthetic tap or phantom ink; clear settings names | Gesture action triggers once; zoom keeps ink anchored |
| NP-028 | P1 | Stylus side button/rear eraser mappings | SOURCE | Test reported button codes and tool switching on S Pen tablet | Configured action works and restores previous tool |
| NP-029 | P1 | One-tap Read/Pan vs Annotate on PDFs | PUBLISHED | B08 `5633d01` pushed; CI/device status must be checked | Read pans, Annotate restores actual last tool |
| NP-030 | P1 | Pen, dashed, calligraphy, taper and speed tools | SOURCE | Check each width/pressure/shape on phone and tablet | Strokes visually distinct and retain settings |
| NP-031 | P1 | Highlighter transparency and inverse mode | SOURCE | Validate compositing against dark/light paper and PDF background | Highlight is consistent with/without inverted paper |
| NP-032 | P1 | Three saved configurations per pen | LOCAL | B10 `65d1aaa` committed; verify settings roundtrip and popup | Save/apply/delete 3 slots per tool after restart |
| NP-033 | P2 | Floating favorite pen dock | SOURCE | B03 implementation; validate grip, collapsed width and overlap | Dock remains tappable across orientations |
| NP-034 | P2 | Color overrides, custom colors and recents | SOURCE | Ensure saved pen color remains fixed when toolbar color changes | Color override and recents survive reopening |
| NP-035 | P2 | Movable toolbar and visible tool states | SOURCE | B01/B02; verify dragging never leaves screen or draws ink | Tool drag is separate from canvas ink |
| NP-036 | P2 | Shape recognition/hold-to-snap | SOURCE | Test false positives, undo and page transforms | Intentional hold shapes recognized; freehand retained otherwise |
| NP-037 | P1 | Text, image, sticker, screenshot, hyperlink tools | PARTIAL | B19 preserves exported image URI files; B20 adds GL decode retry; verify clipboard and image roundtrip on Android | Each object can be edited, moved and restored |
| NP-038 | P2 | Audio recording and synchronized playback | SOURCE | Validate permission denial and clip lifecycle | Playback works after relaunch; no lost recording |

## Eraser, selection, layers and undo

Evidence and NotesTaking targets: N13, N21, N26, N32, N33 · `core/model/Stroke.kt`, `core/tools/ToolConfig.kt`, `core/infinite/EraseSession.kt`, `canvas/InteractionController.kt`, `ui/Popups.kt`.

| ID | Priority | Reference feature / requirement | State | Next implementation or audit | Acceptance gate |
|---|---|---|---|---|---|
| NP-039 | P0 | Point/area eraser splits ink into surviving segments | SOURCE | Check swept clipping, pressure interpolation and double crossing | Middle removed, ends remain editable at all zooms |
| NP-040 | P0 | Whole-stroke eraser removes full intersected line | SOURCE | Preserve separate mode and its current persisted preference | Touch removes entire stroke, no neighboring strokes |
| NP-041 | P1 | Selection/lasso-based erasing semantics | PARTIAL | Lasso selection exists; clarify delete-selection vs eraser modes | Selected objects only are removed; undo one step |
| NP-042 | P1 | Eraser size slider and cursor ring | SOURCE | Validate ring matches effective width at changing zoom | Displayed radius matches removed segment width |
| NP-043 | P2 | Pressure-sensitive eraser size | GAP | Design optional pressure width after hardware calibration | Pressure on/off has measured reversible effect |
| NP-044 | P2 | Erase only marker/highlighter content | GAP | Add independent content filter without deleting PDF source | Marker removed while pen and source PDF unchanged |
| NP-045 | P2 | Erase only tape/selected object types | GAP | Provide safe type filters with explicit active indication | Only permitted types erased in mixed layer test |
| NP-046 | P2 | Eraser affects PDF markup when opted in | SOURCE | Verify toggle and that source PDF text remains untouched | Markup erased/not erased exactly per setting |
| NP-047 | P0 | Area eraser flicker under dense ink | LOCAL | B15 bounded repaint for plain pen strokes added; dense-PDF flicker reproduction and hardware traces still required | No flash/blank region while continuous erasing |
| NP-048 | P1 | One erase sweep is one undo transaction | SOURCE | Verify multi-crossing and cancel paths | Undo once restores all pieces; Redo identical |
| NP-049 | P1 | Separate Undo/Redo stack and branch policy | SOURCE | Check per-pane focus and redo invalidation | Undo/redo deterministic, no other pane modified |
| NP-050 | P2 | Layer management UI, hide, lock and reorder | PARTIAL | PageLayers render ordering exists; full named-layer editor needs audit | Hidden/locked layer preserved and excluded from erase |
| NP-051 | P2 | Lasso select/resize/recolor/copy to another page | SOURCE | Test transform and object IDs in history | Editable selected objects retain positions and undo |
| NP-052 | P1 | Selection and eraser accessibility labels | PARTIAL | Verify tool names, spoken selection state and 48dp hit boxes | TalkBack announces mode/size and can switch both |

## PDF study, page navigation and split workspace

Evidence and NotesTaking targets: N18, N21, N29, N30, N33 · `platform/Pdf.kt`, `ui/SidePanel.kt`, `ui/PageExportDialog.kt`, `ui/Editor.kt`, `core/model/Page.kt`.

| ID | Priority | Reference feature / requirement | State | Next implementation or audit | Acceptance gate |
|---|---|---|---|---|---|
| NP-053 | P0 | Import PDF with Android Storage Access Framework | PARTIAL | B29 CI passed for page-size validation and resource cleanup; B30 CI-verified multi-import progress/outcome accuracy; B31 CI-verified early single-import cancellation; B32 CI-verified invalid page-geometry rejection; B33 CI-verified specific PDF staging failures; B34 CI-verified inserted-PDF geometry and cleanup; B35 CI-verified bounded page-size reads; B36 locally rejects oversized combined PDF joins and malformed join geometry before PDFBox; test providers and large PDFs | Import preserves original source and can reopen |
| NP-054 | P1 | Protected or malformed PDF failure handling | PARTIAL | B29 CI passed incomplete geometry rejection; B30 CI-verified batch failure/cancellation feedback; B31 CI-verified stale password-error reset; B32 CI-verified malformed MediaBox rejection; B33 CI-verified source/format/size/password feedback; B34 CI-verified malformed page rejection during insertion; B35 CI-verified exact native geometry slices; B36 locally rejects malformed PDF join inputs before mutation; physical provider QA remains pending | Wrong password/invalid file never corrupts note |
| NP-055 | P0 | PDF page overlay coordinates at any zoom/rotation | SOURCE | Reproduce crop/rotation/margin cases with landmark ink | Ink stays on landmark at 50–400% zoom/reopen/export |
| NP-056 | P1 | Vector-backed PDF background render | SOURCE | Keep source PDF vector and bounded memory; measure on device | Text stays sharp, no 200-page OOM |
| NP-057 | P1 | Single/multi-page thumbnail navigation | SOURCE | B06 page manager improved; verify page index after reorder | Thumbnail jumps correctly with stable cover handling |
| NP-058 | P1 | Insert blank/template pages into PDF note | PARTIAL | B34 CI-verified inserted-PDF geometry and failed-join cleanup; verify destination order, sizes, undo and original source integrity | Mixed PDF/blank pages persist and export |
| NP-059 | P2 | Duplicate, move, rotate and delete page | SOURCE | Check undo and protected cover action | Correct order, stable annotations and source copy |
| NP-060 | P2 | Multi-select page operations | SOURCE | Verify Save As, copy/delete actions and clear selection | Selected pages only affected; undo works |
| NP-061 | P2 | Page manager annotation list/tab | INVESTIGATE | Notein has All Pages/Annotations; audit NotesTaking sidebar coverage | Annotation list navigates and filters by page |
| NP-062 | P1 | Save selected page(s) as a new editable note | SOURCE | Existing Extract to new note menu supports single/multi-page native `.xnote`; B37 locally guards cancellation and adds embedded-asset roundtrip tests; real provider QA pending | New note independent; original unchanged |
| NP-063 | P2 | Page Bookmarks, contents and outline navigation | SOURCE | Verify row state after insertion and PDF bookmark import | Jump uses correct page and survives reopen |
| NP-064 | P2 | PDF text highlighting, underline, strikeout | SOURCE | Check actual markup geometry and export semantics | Markup aligns after rotations and undo |
| NP-065 | P1 | PDF text search and search-result jump | SOURCE | Verify PDFium text with non-Latin and rotated pages | Next/previous jumps to correct visible hit |
| NP-066 | P1 | Reading mode hand/Pan tool shortcuts | PUBLISHED | B08 `5633d01`; keep PDF zoom and bookmark states | Switching Read/Annotate does not reset view |
| NP-067 | P2 | Split two notes side-by-side | SOURCE | Test divider hit areas, pane focus and gesture ownership | Each pane retains independent page/undo |
| NP-068 | P2 | Page margins, crop/rotation template overlays | SOURCE | Check inserted paper size and PDF page turn matrix | Annotations and thumbnail/export align |
| NP-069 | P2 | Internal links and page hyperlinks | SOURCE | Verify tap/open/copy and PDF export links | Link works without corrupting stored note |
| NP-070 | P1 | 200+ page PDF performance and viewport retention | INVESTIGATE | B35 CI-verified geometry limit; B36 locally prevents over-limit PDFBox joins/subsets; still measure memory, scroll latency and cache thrash on device | No repeated full re-render or lost pen gesture |

## Export, native notes, backups and durability

Evidence and NotesTaking targets: N18, N26, N28, N30, N33 · `ui/PageExportDialog.kt`, `platform/Pdf.kt`, `format/DocumentCodec.kt`, `format/BoundedZipInputStream.kt`, `MainActivity.kt`.

| ID | Priority | Reference feature / requirement | State | Next implementation or audit | Acceptance gate |
|---|---|---|---|---|---|
| NP-071 | P0 | Native .xnote/.xcanvas saves preserve editable strokes | SOURCE | Retain current native format; B37 adds extracted `.xnote` ink/asset roundtrip regression; still test old fixtures and `.xcanvas` on device | Sample/pressure/page content identical after reopen |
| NP-072 | P1 | Export this/selected/all/custom page ranges | PARTIAL | B07 scope selector; B26 CI-verified truthful multi-PNG counts on partial provider failure; verify page order, invalid inputs and device output | Exact PDF/PNG page count, no partial range export |
| NP-073 | P1 | PDF vs editable native-note explanation | LOCAL | B11: shareable PDF copy now explains native stroke editability; pending CI and phone review | User understands what can be edited after saving |
| NP-074 | P1 | Separate truly editable PDF annotation export | GAP | Research PDFBox annotation mapping and Acrobat/Xodo interoperability | Exported annotations independently editable in other readers |
| NP-075 | P2 | JPEG single/multi-page export format | GAP | Add JPEG only after format encoder + MIME/SAF support | Actual JPEG opens in gallery, correct colors/quality |
| NP-076 | P2 | Export background/paper inclusion toggle | GAP | Implement renderer option and preview; don't fake toggle | PDF/png includes or excludes background accurately |
| NP-077 | P2 | Export outlines/bookmarks and markup inclusion flags | PARTIAL | Existing PDF exporter preserves bookmarks; add selectable flags if safe | Each flag alters exported output, not stored note |
| NP-078 | P2 | Export strokes/images/text inclusion toggle | GAP | Add independently tested render flags | Chosen categories excluded without source mutation |
| NP-079 | P2 | Extra page margins inclusion option | GAP | Define geometry and output page size independent of stored page | PDF page boxes and overlay positions match choice |
| NP-080 | P3 | Watermark text/image for exported PDF | GAP | Add optional previewed watermark only with verified PDF path | Correct watermark at all page sizes, no source alteration |
| NP-081 | P0 | SAF Save As/Share URI, MIME and write completion | PARTIAL | B21/B23–B28 CI-verified null-stream, provider output, partial cleanup and occupied/unreadable destination guards; B42 stalled-zero-read detection and byte-integrity regression passed CI; B43 zero-size SAF destination verification passed CI; device compatibility QA pending; Chat A provider-sidecar null-stream guard passed GitHub CI #43572100224 on `cfadefe`; provider retry and device acceptance remain open | Only report success after durable, complete bytes |
| NP-082 | P0 | Export progress, cancel and temporary file cleanup | PARTIAL | B25/B26 staged PDF/PNG cleanup and B38 Extract/Merge native asset cancellation passed CI; B42 provider-copy stalled-read detection and error cleanup passed GitHub CI; B44 final PDF/ZIP destination-copy cancellation passed GitHub CI at `9729c7c`; device cancel, disk-full and provider tests pending | No corrupt output or abandoned large staging files |
| NP-083 | P0 | Safe native ZIP import and resource limits | SOURCE | BoundedZipInputStream exists; execute malformed/traversal/bomb fixtures in CI | Traversal rejected; original/staging untouched |
| NP-084 | P0 | Atomic local save and document recovery | PARTIAL | B18 recovery/overwrite tests and B27–B28 occupied SAF protection passed CI; B39 zero-byte atomic-save protection passed CI; B41 bulk-byte asset output passed CI at `fd72e00`; Chat A B45/B46 metadata preflight and native ZIP structure checks passed GitHub CI #43566218915 on `0ac256e`; Chat A B47/B48 blocked-directory and empty-native-file metadata-only preflight passed GitHub CI #43570476224 on `1280b8b`; document-plus-sidecar writes still are not a cross-file atomic transaction; force-stop and low-storage device QA remain | Old valid note survives interrupted write |
| NP-085 | P1 | Save Page As creates independent native note | SOURCE | Existing `extractPages`/`writeCargoNote` uses PageCargo and native DocumentCodec; B37 asset roundtrip and B38 early Cancel/overlapping Extract/Merge safeguards passed CI; phone/tablet provider and Move QA pending | New page note is editable; source remains unchanged |
| NP-086 | P2 | Version history/recovery snapshot UI | GAP | Design local version snapshots with quotas and restore preview | Restore past version without touching unrelated notes |
| NP-087 | P2 | Local backup, settings restore and conflict resolution | PARTIAL | B12 `a8647c5` locally hardens settings ZIP read/skip budgets; native note-level backup/restore and provider QA remain open | Restore into test folder, no silent overwrite |
| NP-088 | P3 | Optional cloud backup/sync | OPTIONAL | Needs separate secure backend, consent, E2E policy and conflict model | Offline remains fully functional; sync opt-in only |

## Navigation, responsive UX and accessibility

Evidence and NotesTaking targets: N29, N30, N31, N32, N33 · `ui/Toolbar.kt`, `ui/FloatingToolbar.kt`, `ui/PreferencesPane.kt`, `ui/SidePanel.kt`, `ui/theme/*`.

| ID | Priority | Reference feature / requirement | State | Next implementation or audit | Acceptance gate |
|---|---|---|---|---|---|
| NP-089 | P1 | 48dp touch regions on primary actions | PARTIAL | B01–B07 increased many; B45 paged toolbar title/page/zoom 48dp text targets passed CI (device QA pending); full-app phone/tablet audit remains | All finger actions have ≥48dp hit area |
| NP-090 | P1 | TalkBack labels and active/selected state | PARTIAL | B40 selected-state semantics passed GitHub CI; B50 page/zoom spoken values passed GitHub CI #43568212147 on `3df2b04`; TalkBack device QA and remaining icon/preset/dialog audit pending | Every icon is discoverable, action/state spoken |
| NP-091 | P1 | Phone/tablet/landscape responsive editor | SOURCE | Measure portrait/landscape, split/multi-window and keyboard | No clipped toolbar or inaccessible Save |
| NP-092 | P1 | Large-font settings and keyboard/back navigation | PARTIAL | B46 CI-passed stacked tooltip shortcuts at ≥1.3 font scale; B47 CI-passed floating quick-action scroll with split Close Pane pinned; B48 CI-passed collapsed Expand accessibility for narrow panes; B49 keyboard toggle and Compose imports passed CI #43416921904; B51 keyboard focus outline passed CI #43569440413 on `b751ff1`; B52 shared toolbar focus passed CI #43573157803 on `d4741c8`; B53 Add Section focus/role passed CI #43573959181 on `efd1e58`; B54 grip movement availability passed CI #43576715502 on `c86ce7c`; B55 Reset suppression passed CI #43581363803 on `79f7700`; B56 infinite All Tools Fit guard passed CI #43584553499 on `d3de83b`; B57 paged Rename label/IME Done passed CI #43585453675 on `e844af0`; B58 paged/PDF Fit zoom-lock guard passed CI #43586268815 on `66d70b0`; B59 vertical canvas warnings passed CI #43587081045 on `58cbed0`; B60 vertical note-title passed CI #43588674901 on `e863c2a`; B61 vertical title/warning focus passed CI #43589804366 on `d0ec195`; B62 horizontal focus/tooltips published at `42069f2` with CI #43590738780 in progress; B63 grip-arrow routing candidate local for coordinator review only; 320dp/1.3x/2x keyboard and TalkBack device QA still pending; settings, focus and system Back remain open | No truncated critical action at 320dp |
| NP-093 | P2 | Organize many toolbar settings by category/search | GAP | Group visibility/action settings with preview and optional search | Core tools located quickly, no preference reset |
| NP-094 | P2 | Reading-first UI without toolbox overlap | PARTIAL | B08 Read/Annotate is tool switch; full immersive layout deferred | Reading leaves ample page room; no hidden navigation |
| NP-095 | P2 | Light, Dark, OLED, dynamic and custom colors | SOURCE | Retain saved theme roles; measure contrast on device | Accessible text/icons in each supported theme |
| NP-096 | P2 | Reduced motion and interruption-safe animations | INVESTIGATE | Check system animation-scale handling before adding transitions | No delay or ink interruption; reduce-motion honored |
| NP-097 | P2 | Tooltips and contextual first-use guidance | PARTIAL | Provide optional, dismissible guidance for writing/gesture/eraser | Novice can write/erase/export without blocking canvas |
| NP-098 | P2 | Responsive split-pane divider and easy switch | PARTIAL | Audit effective 48dp drag affordance, toolbar edges | Divider resizes without drawing or switching unintended pane |
| NP-099 | P2 | Color/width/pressure preview with accessible names | PARTIAL | Existing pen popup; B10 saved styles; test label and previews | Preset names and value changes announced |
| NP-100 | P1 | Accurate save, undo, export status and feedback | PARTIAL | B59 vertical GL/low-memory warnings published for CI #43587081045; remaining error/success/cancellation UI audit open | No false success, clear retry on provider failure |

## Optional/reference-only features and non-goals

Evidence and NotesTaking targets: N29–N33 · Existing `docs/canvas/FEATURE_BACKLOG.md` and offline-first design plan.

| ID | Priority | Reference feature / requirement | State | Next implementation or audit | Acceptance gate |
|---|---|---|---|---|---|
| NP-101 | P2 | Offline printed/scanned PDF OCR | GAP | Feasibility/design privacy: bundled/downloaded model and page-index contract | Offline OCR text searchable with confidence and rotation |
| NP-102 | P3 | Stylus handwriting-to-text recognition | GAP | Research model availability and input grouping for English/Hindi | Converts test strokes without replacing originals |
| NP-103 | P3 | AI summarization or assistant | OPTIONAL | Do not require cloud login/API; define explicit privacy and costs first | Core note app remains usable with feature disabled |
| NP-104 | P3 | AI-created templates or study flashcards | OPTIONAL | Consider local template generator separate from AI network features | Reproducible editable output with offline fallback |
| NP-105 | P3 | Translation and smart study selection | OPTIONAL | Reuse selectable PDF text; no account requirement | English/Hindi result while preserving original selection |
| NP-106 | P2 | Note lock or secure notebook | GAP | Design key management, backup/recovery, encryption at rest | Correct lock/unlock without data-loss or false security claims |
| NP-107 | P3 | Subscription/storefront and commercial packs | OPTIONAL | Not part of offline-first core parity; do not add by default | No checkout/paywall introduced unintentionally |
| NP-108 | P3 | Account, Google/Microsoft login | OPTIONAL | Only if a separately specified sync identity product is approved | Guest mode unaffected; privacy/security reviewed |
| NP-109 | P2 | True tablet/S Pen benchmark suite | INVESTIGATE | Hardware test across stylus pressure, palm, zoom and low memory | Device logs/screenshots and timings per TEST_PLAN |
| NP-110 | P2 | Independent app artwork, icons and motion | SOURCE | Keep NotesTaking branding; never copy Notein graphics/assets | Own visual identity across dark/light/launcher |

## Updating this checklist

At every implementation batch, cite NP IDs in the commit message and [PROGRESS.md](PROGRESS.md); update the state in the table only when source or CI evidence changes. After **exact SHA** CI succeeds, record Android test/lint/APK results; after applicable **real-device, output-file and TalkBack** checks succeed, log VERIFIED and evidence paths. **Never mark the whole category done based on a local source diff.** For detailed manual steps use [TEST_PLAN.md](TEST_PLAN.md).

There are **110** tracked requirements. They include optional/feasibility candidates deliberately segregated from offline core work. The original historical audit workbook is not modified or automatically closed.
