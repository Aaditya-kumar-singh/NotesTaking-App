# NotesTaking App - A-Z pre-APK task list

This is the complete **planning queue**, not evidence that tasks have failed or passed. Many SOURCE/PARTIAL rows already have CI-tested code. See the original NP matrix, NT plan, and the live coordinator before making changes.

**26 batches A-Z, exactly five tasks each (130 tasks):** 110 NP feature checks and 20 release-readiness checks. Priority is P0 -> P1 -> P2 -> P3, with release gates W-Z. Beta builds can continue before all tasks are accepted; P2/P3 scope requires explicit deferral decisions, never silent removal.

Owner is **preferred routing**, not file ownership; COORD/DEVICE/GEMINI tasks are not automatically assigned to GPT A/B/C. Any change touching unowned files needs coordinator-approved allocation. ChatGPT sessions do not run without a user turn. Review [the three-chat protocol](THREE_GPT_APK_WORKFLOW.md).

## Batch A

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-020 | P0 | C | SOURCE | Stylus takes exclusive ink ownership - No stroke broken by incidental touch or pinch | NT-019 |
| 2 | NP-021 | P0 | C | SOURCE | Finger-first then stylus-first mixed pointer handoff - Stylus starts/finishes normally with resting finger | - |
| 3 | NP-022 | P0 | C | PUBLISHED | Stylus/eraser tip bypasses page scrollbar drag - Pen and eraser draw beside scrollbar; finger scrolling still works | NT-020, NT-021, NT-022 |
| 4 | NP-023 | P0 | C | SOURCE | Pressure-sensitive pen strokes - Pressure affects resulting stroke at 25–400% zoom | NT-023 |
| 5 | NP-039 | P0 | C | SOURCE | Point/area eraser splits ink into surviving segments - Middle removed, ends remain editable at all zooms | NT-031 |

## Batch B

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-040 | P0 | C | SOURCE | Whole-stroke eraser removes full intersected line - Touch removes entire stroke, no neighboring strokes | NT-032 |
| 2 | NP-047 | P0 | C | LOCAL | Area eraser flicker under dense ink - No flash/blank region while continuous erasing | NT-038 |
| 3 | NP-053 | P0 | A | PARTIAL | Import PDF with Android Storage Access Framework - Import preserves original source and can reopen | NT-043 |
| 4 | NP-055 | P0 | A | SOURCE | PDF page overlay coordinates at any zoom/rotation - Ink stays on landmark at 50–400% zoom/reopen/export | NT-044, NT-045 |
| 5 | NP-071 | P0 | A | SOURCE | Native .xnote/.xcanvas saves preserve editable strokes - Sample/pressure/page content identical after reopen | NT-054 |

## Batch C

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-081 | P0 | A | PARTIAL | SAF Save As/Share URI, MIME and write completion - Only report success after durable, complete bytes | NT-060 |
| 2 | NP-082 | P0 | A | PARTIAL | Export progress, cancel and temporary file cleanup - No corrupt output or abandoned large staging files | NT-061 |
| 3 | NP-083 | P0 | A | SOURCE | Safe native ZIP import and resource limits - Traversal rejected; original/staging untouched | NT-062 |
| 4 | NP-084 | P0 | A | PARTIAL | Atomic local save and document recovery - Old valid note survives interrupted write | NT-063 |
| 5 | NP-001 | P1 | COORD | SOURCE | No mandatory sign-in for core note creation - Create and reopen an offline note without network/account | NT-010 |

## Batch D

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-003 | P1 | COORD | SOURCE | Fast launch into recent/home library - Reopen last note or create one in fewer than three actions | NT-011 |
| 2 | NP-004 | P1 | GEMINI | GAP | Quick Scratch new blank note affordance - Tap opens writable note; back saves/asks safely | - |
| 3 | NP-005 | P1 | COORD | SOURCE | Standard Note and Infinite Canvas creation - Both create valid documents in portrait and landscape | NT-012 |
| 4 | NP-006 | P1 | A | SOURCE | Create folders and rename/move notes - Move/rename no duplicate/lost content | - |
| 5 | NP-009 | P1 | COORD | SOURCE | Paper/cover template preview - Preview equals created cover/paper appearance | NT-013 |

## Batch E

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-013 | P1 | COORD | SOURCE | Search library by title/type - Exact/partial search yields correct note | NT-014 |
| 2 | NP-017 | P1 | A | PARTIAL | Recent items and Trash recovery - Restore note without lost attachments; delete is deliberate | NT-016 |
| 3 | NP-019 | P1 | COORD | SOURCE | Accessible create menu and empty-library recovery - Create/Import accessible at phone/tablet sizes | NT-018 |
| 4 | NP-025 | P1 | C | SOURCE | Smooth in-progress ink with commit on pen-up - No disappearing ink, doubling or late commit | - |
| 5 | NP-026 | P1 | C | LOCAL | Stylus-only ink vs finger draw setting - Finger pans in stylus-only mode; can draw when enabled | NT-025 |

## Batch F

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-027 | P1 | C | SOURCE | Two/three finger gestures and pinch zoom - Gesture action triggers once; zoom keeps ink anchored | - |
| 2 | NP-028 | P1 | C | SOURCE | Stylus side button/rear eraser mappings - Configured action works and restores previous tool | - |
| 3 | NP-029 | P1 | C | PUBLISHED | One-tap Read/Pan vs Annotate on PDFs - Read pans, Annotate restores actual last tool | NT-026 |
| 4 | NP-030 | P1 | C | SOURCE | Pen, dashed, calligraphy, taper and speed tools - Strokes visually distinct and retain settings | NT-027 |
| 5 | NP-031 | P1 | C | SOURCE | Highlighter transparency and inverse mode - Highlight is consistent with/without inverted paper | - |

## Batch G

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-032 | P1 | B | LOCAL | Three saved configurations per pen - Save/apply/delete 3 slots per tool after restart | NT-008, NT-028 |
| 2 | NP-037 | P1 | B | PARTIAL | Text, image, sticker, screenshot, hyperlink tools - Each object can be edited, moved and restored | - |
| 3 | NP-041 | P1 | C | PARTIAL | Selection/lasso-based erasing semantics - Selected objects only are removed; undo one step | NT-033 |
| 4 | NP-042 | P1 | C | SOURCE | Eraser size slider and cursor ring - Displayed radius matches removed segment width | NT-034 |
| 5 | NP-048 | P1 | C | SOURCE | One erase sweep is one undo transaction - Undo once restores all pieces; Redo identical | NT-039 |

## Batch H

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-049 | P1 | C | SOURCE | Separate Undo/Redo stack and branch policy - Undo/redo deterministic, no other pane modified | NT-040 |
| 2 | NP-052 | P1 | B | PARTIAL | Selection and eraser accessibility labels - TalkBack announces mode/size and can switch both | - |
| 3 | NP-054 | P1 | A | PARTIAL | Protected or malformed PDF failure handling - Wrong password/invalid file never corrupts note | - |
| 4 | NP-056 | P1 | C | SOURCE | Vector-backed PDF background render - Text stays sharp, no 200-page OOM | - |
| 5 | NP-057 | P1 | A | SOURCE | Single/multi-page thumbnail navigation - Thumbnail jumps correctly with stable cover handling | NT-046 |

## Batch I

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-058 | P1 | A | PARTIAL | Insert blank/template pages into PDF note - Mixed PDF/blank pages persist and export | - |
| 2 | NP-062 | P1 | A | SOURCE | Save selected page(s) as a new editable note - New note independent; original unchanged | NT-048 |
| 3 | NP-065 | P1 | A | SOURCE | PDF text search and search-result jump - Next/previous jumps to correct visible hit | - |
| 4 | NP-066 | P1 | B | PUBLISHED | Reading mode hand/Pan tool shortcuts - Switching Read/Annotate does not reset view | NT-050 |
| 5 | NP-070 | P1 | C | INVESTIGATE | 200+ page PDF performance and viewport retention - No repeated full re-render or lost pen gesture | NT-053 |

## Batch J

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-072 | P1 | A | PARTIAL | Export this/selected/all/custom page ranges - Exact PDF/PNG page count, no partial range export | NT-009, NT-055 |
| 2 | NP-073 | P1 | A | LOCAL | PDF vs editable native-note explanation - User understands what can be edited after saving | NT-056 |
| 3 | NP-074 | P1 | A | PARTIAL | Separate truly editable PDF annotation export - Exported annotations independently editable in other readers | NT-057 |
| 4 | NP-085 | P1 | A | SOURCE | Save Page As creates independent native note - New page note is editable; source remains unchanged | NT-064 |
| 5 | NP-089 | P1 | B | PARTIAL | 48dp touch regions on primary actions - All finger actions have ≥48dp hit area | NT-067 |

## Batch K

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-090 | P1 | B | PARTIAL | TalkBack labels and active/selected state - Every icon is discoverable, action/state spoken | NT-068 |
| 2 | NP-091 | P1 | B | SOURCE | Phone/tablet/landscape responsive editor - No clipped toolbar or inaccessible Save | NT-069 |
| 3 | NP-092 | P1 | B | PARTIAL | Large-font settings and keyboard/back navigation - No truncated critical action at 320dp | NT-070 |
| 4 | NP-100 | P1 | B | PARTIAL | Accurate save, undo, export status and feedback - No false success, clear retry on provider failure | - |
| 5 | NP-002 | P2 | COORD | INVESTIGATE | Optional short onboarding with Skip - Skip is visible, survives restart, no consent automatically granted | - |

## Batch L

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-007 | P2 | A | SOURCE | Import documents directly from Create menu - Import valid PDF/notes; fail cleanly on unsupported DOCX | - |
| 2 | NP-008 | P2 | A | SOURCE | Import image/media into editor - Image/audio embedded and survives reopen | NT-017 |
| 3 | NP-010 | P2 | A | SOURCE | A4, custom sizes and orientation - Custom page dimensions persist across reopen/export | - |
| 4 | NP-011 | P2 | COORD | SOURCE | Ruled, dotted, grid and blank template choices - All available templates render and persist consistently | - |
| 5 | NP-012 | P2 | A | SOURCE | Notebook title and safe naming - Named note opens without overwriting another | - |

## Batch M

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-014 | P2 | COORD | SOURCE | Sort and grid/gallery/list toggles - Correct order and no lost selection after toggle | - |
| 2 | NP-015 | P2 | COORD | INVESTIGATE | Favorites/stars for notes (not favorite pens) - Starred filter updates and persists across relaunch | NT-015 |
| 3 | NP-016 | P2 | COORD | PARTIAL | Labels/tags and color filters - Tag add/remove/filter with safe migration and multi-note handling | - |
| 4 | NP-018 | P2 | A | SOURCE | Bulk copy/move/export/delete in library - N selected documents processed exactly once with errors | - |
| 5 | NP-024 | P2 | C | INVESTIGATE | Tilt/orientation brush response - Supported tablet responds; unsupported device degrades safely | NT-024 |

## Batch N

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-033 | P2 | B | SOURCE | Floating favorite pen dock - Dock remains tappable across orientations | NT-029 |
| 2 | NP-034 | P2 | B | SOURCE | Color overrides, custom colors and recents - Color override and recents survive reopening | - |
| 3 | NP-035 | P2 | B | SOURCE | Movable toolbar and visible tool states - Tool drag is separate from canvas ink | - |
| 4 | NP-036 | P2 | C | SOURCE | Shape recognition/hold-to-snap - Intentional hold shapes recognized; freehand retained otherwise | NT-030 |
| 5 | NP-038 | P2 | C | SOURCE | Audio recording and synchronized playback - Playback works after relaunch; no lost recording | - |

## Batch O

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-043 | P2 | C | GAP | Pressure-sensitive eraser size - Pressure on/off has measured reversible effect | NT-035 |
| 2 | NP-044 | P2 | C | GAP | Erase only marker/highlighter content - Marker removed while pen and source PDF unchanged | NT-036 |
| 3 | NP-045 | P2 | C | GAP | Erase only tape/selected object types - Only permitted types erased in mixed layer test | - |
| 4 | NP-046 | P2 | C | SOURCE | Eraser affects PDF markup when opted in - Markup erased/not erased exactly per setting | NT-037 |
| 5 | NP-050 | P2 | C | PARTIAL | Layer management UI, hide, lock and reorder - Hidden/locked layer preserved and excluded from erase | NT-041 |

## Batch P

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-051 | P2 | C | SOURCE | Lasso select/resize/recolor/copy to another page - Editable selected objects retain positions and undo | NT-042 |
| 2 | NP-059 | P2 | A | SOURCE | Duplicate, move, rotate and delete page - Correct order, stable annotations and source copy | - |
| 3 | NP-060 | P2 | A | SOURCE | Multi-select page operations - Selected pages only affected; undo works | - |
| 4 | NP-061 | P2 | A | INVESTIGATE | Page manager annotation list/tab - Annotation list navigates and filters by page | NT-047 |
| 5 | NP-063 | P2 | A | SOURCE | Page Bookmarks, contents and outline navigation - Jump uses correct page and survives reopen | NT-049 |

## Batch Q

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-064 | P2 | A | SOURCE | PDF text highlighting, underline, strikeout - Markup aligns after rotations and undo | - |
| 2 | NP-067 | P2 | C | SOURCE | Split two notes side-by-side - Each pane retains independent page/undo | NT-051 |
| 3 | NP-068 | P2 | C | SOURCE | Page margins, crop/rotation template overlays - Annotations and thumbnail/export align | NT-052 |
| 4 | NP-069 | P2 | C | SOURCE | Internal links and page hyperlinks - Link works without corrupting stored note | - |
| 5 | NP-075 | P2 | C | GAP | JPEG single/multi-page export format - Actual JPEG opens in gallery, correct colors/quality | NT-058 |

## Batch R

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-076 | P2 | C | GAP | Export background/paper inclusion toggle - PDF/png includes or excludes background accurately | NT-059 |
| 2 | NP-077 | P2 | C | PARTIAL | Export outlines/bookmarks and markup inclusion flags - Each flag alters exported output, not stored note | - |
| 3 | NP-078 | P2 | C | GAP | Export strokes/images/text inclusion toggle - Chosen categories excluded without source mutation | - |
| 4 | NP-079 | P2 | C | GAP | Extra page margins inclusion option - PDF page boxes and overlay positions match choice | - |
| 5 | NP-086 | P2 | A | GAP | Version history/recovery snapshot UI - Restore past version without touching unrelated notes | NT-065 |

## Batch S

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-087 | P2 | A | PARTIAL | Local backup, settings restore and conflict resolution - Restore into test folder, no silent overwrite | - |
| 2 | NP-093 | P2 | B | GAP | Organize many toolbar settings by category/search - Core tools located quickly, no preference reset | NT-071 |
| 3 | NP-094 | P2 | B | PARTIAL | Reading-first UI without toolbox overlap - Reading leaves ample page room; no hidden navigation | NT-072 |
| 4 | NP-095 | P2 | B | SOURCE | Light, Dark, OLED, dynamic and custom colors - Accessible text/icons in each supported theme | NT-073 |
| 5 | NP-096 | P2 | B | INVESTIGATE | Reduced motion and interruption-safe animations - No delay or ink interruption; reduce-motion honored | NT-074 |

## Batch T

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-097 | P2 | B | PARTIAL | Tooltips and contextual first-use guidance - Novice can write/erase/export without blocking canvas | NT-075 |
| 2 | NP-098 | P2 | B | PARTIAL | Responsive split-pane divider and easy switch - Divider resizes without drawing or switching unintended pane | NT-076 |
| 3 | NP-099 | P2 | B | PARTIAL | Color/width/pressure preview with accessible names - Preset names and value changes announced | - |
| 4 | NP-101 | P2 | A | GAP | Offline printed/scanned PDF OCR - Offline OCR text searchable with confidence and rotation | NT-077 |
| 5 | NP-106 | P2 | A | GAP | Note lock or secure notebook - Correct lock/unlock without data-loss or false security claims | - |

## Batch U

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-109 | P2 | COORD | INVESTIGATE | True tablet/S Pen benchmark suite - Device logs/screenshots and timings per TEST_PLAN | - |
| 2 | NP-110 | P2 | COORD | SOURCE | Independent app artwork, icons and motion - Own visual identity across dark/light/launcher | - |
| 3 | NP-080 | P3 | C | GAP | Watermark text/image for exported PDF - Correct watermark at all page sizes, no source alteration | - |
| 4 | NP-088 | P3 | A | OPTIONAL | Optional cloud backup/sync - Offline remains fully functional; sync opt-in only | NT-066 |
| 5 | NP-102 | P3 | C | GAP | Stylus handwriting-to-text recognition - Converts test strokes without replacing originals | NT-078 |

## Batch V

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | NP-103 | P3 | COORD | OPTIONAL | AI summarization or assistant - Core note app remains usable with feature disabled | NT-079 |
| 2 | NP-104 | P3 | COORD | OPTIONAL | AI-created templates or study flashcards - Reproducible editable output with offline fallback | - |
| 3 | NP-105 | P3 | COORD | OPTIONAL | Translation and smart study selection - English/Hindi result while preserving original selection | - |
| 4 | NP-107 | P3 | COORD | OPTIONAL | Subscription/storefront and commercial packs - No checkout/paywall introduced unintentionally | - |
| 5 | NP-108 | P3 | COORD | OPTIONAL | Account, Google/Microsoft login - Guest mode unaffected; privacy/security reviewed | - |

## Batch W

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | REL-01 | P0 | COORD | NOT_VERIFIED | Audit exact developer SHA and required CI jobs - Evidence signed off by coordinator; missing physical tests recorded as blockers. | NT-001 |
| 2 | REL-02 | P0 | COORD | NOT_VERIFIED | Reconcile three agent sessions and active file claims - Evidence signed off by coordinator; missing physical tests recorded as blockers. | NT-002 |
| 3 | REL-03 | P0 | COORD | NOT_VERIFIED | Review all P0/P1 feature evidence and open exceptions - Evidence signed off by coordinator; missing physical tests recorded as blockers. | NT-003 |
| 4 | REL-04 | P0 | A | NOT_VERIFIED | Reopen old xnote/xcanvas/xtemplate fixtures without migration loss - Evidence signed off by coordinator; missing physical tests recorded as blockers. | NT-004 |
| 5 | REL-05 | P0 | A | NOT_VERIFIED | Verify hostile ZIP, encrypted PDF and limit regression coverage - Evidence signed off by coordinator; missing physical tests recorded as blockers. | NT-005 |

## Batch X

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | REL-06 | P0 | A | NOT_VERIFIED | Test PDF import/export and source preservation with SAF - Evidence signed off by coordinator; missing physical tests recorded as blockers. | NT-006 |
| 2 | REL-07 | P0 | A | NOT_VERIFIED | Test force-stop, zero-byte and low-storage save recovery - Evidence signed off by coordinator; missing physical tests recorded as blockers. | NT-007 |
| 3 | REL-08 | P0 | DEVICE | NOT_VERIFIED | Measure 200-page PDFs and memory/jank with Perfetto - Evidence signed off by coordinator; missing physical tests recorded as blockers. | - |
| 4 | REL-09 | P0 | DEVICE | NOT_VERIFIED | Validate palm rejection, mixed touch and stylus pressure - Evidence signed off by coordinator; missing physical tests recorded as blockers. | - |
| 5 | REL-10 | P0 | DEVICE | NOT_VERIFIED | Validate eraser segments, dense ink and undo/redo - Evidence signed off by coordinator; missing physical tests recorded as blockers. | - |

## Batch Y

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | REL-11 | P0 | DEVICE | NOT_VERIFIED | Validate zoom, crop, rotation, split pane and page anchoring - Evidence signed off by coordinator; missing physical tests recorded as blockers. | - |
| 2 | REL-12 | P0 | DEVICE | NOT_VERIFIED | Check TalkBack, keyboard, touch targets and large fonts - Evidence signed off by coordinator; missing physical tests recorded as blockers. | - |
| 3 | REL-13 | P0 | DEVICE | NOT_VERIFIED | Check offline startup, localization and permission-denial cases - Evidence signed off by coordinator; missing physical tests recorded as blockers. | - |
| 4 | REL-14 | P0 | COORD | NOT_VERIFIED | Run and verify full-ABI GitHub CI on exact release candidate SHA - Evidence signed off by coordinator; missing physical tests recorded as blockers. | - |
| 5 | REL-15 | P0 | COORD | NOT_VERIFIED | Review privacy, backup, dependencies and secret hygiene - Evidence signed off by coordinator; missing physical tests recorded as blockers. | - |

## Batch Z

| # | Task ID | Priority | Routed to | Snapshot | Task / acceptance target | Tests |
|---|---|---|---|---|---|---|
| 1 | REL-16 | P0 | COORD | NOT_VERIFIED | Record device QA evidence, outstanding defects and waivers - Evidence signed off by coordinator; missing physical tests recorded as blockers. | - |
| 2 | REL-17 | P0 | COORD | NOT_VERIFIED | Reconcile unique main/master changes; create rollback refs - Evidence signed off by coordinator; missing physical tests recorded as blockers. | - |
| 3 | REL-18 | P0 | COORD | NOT_VERIFIED | Verify release version, tag, keystore secrets and key continuity - Evidence signed off by coordinator; missing physical tests recorded as blockers. | - |
| 4 | REL-19 | P0 | COORD | NOT_VERIFIED | Run signed release workflow; verify signature, ABI and SHA256 - Evidence signed off by coordinator; missing physical tests recorded as blockers. | - |
| 5 | REL-20 | P0 | COORD | NOT_VERIFIED | Review release notes, package distribution and final signoff - Evidence signed off by coordinator; missing physical tests recorded as blockers. | - |

## Acceptance rules

- Never equate a SOURCE, LOCAL, PARTIAL or PUBLISHED tag with an accepted task.
- Every task needs scope/caller review, source checks, a local commit or evidence-only report, coordinator approval and applicable exact-SHA CI.
- Hardware-dependent acceptance requires actual device logs and screenshots; an unavailable device is a blocker to claiming public-release acceptance.
- Independent tasks in later batches may start while another batch is in CI. Only publication, integration and release are serialized.
- Network/CI failures are a separate exclusive incident claim; any slot may investigate and fix within its owned file paths, otherwise request transfer.
- The SQLite ledger controls live claims, reviewer decisions, replacements and incident ownership. Do not edit this Markdown to impersonate progress.
