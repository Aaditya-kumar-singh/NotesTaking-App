# Notein parity: acceptance and regression test plan

**State:** These are **planned test cases**, not test results. Running the source checker does **not** execute these cases. Always use disposable NotesTaking documents and PDF copies; do not interact with real user's private Notein notes or upload them.

**Test environments:** GitHub Actions for Kotlin unit/lint/APK/fixtures; Samsung phone (e.g. SM-M135F) for phone UX; an **actual S Pen-capable tablet** for pressure, palm, rear eraser, hover, and latency; PDF reader (Acrobat/Xodo or independent alternative) for exported annotation interoperability; TalkBack + large text for accessibility. Follow repository no-local-build/install rules.

## Source, build and old-note compatibility

| Test | Priority | Environment | NP IDs | Steps | Pass condition |
|---|---|---|---|---|---|
| NT-001 | P0 | CI | NP-001–NP-110 | Validate project source and XML resources on exact staged commit | No duplicates/missing resources; source checker exits zero |
| NT-002 | P0 | CI | NP-071,NP-083 | Run all Kotlin unit tests, Android lint and debug APK assembly | No compile/unit/lint failure; artifact from exact SHA |
| NT-003 | P0 | CI | NP-071,NP-083 | Reopen old .xnote/.xcanvas/.xtemplate fixtures | Unchanged notes load; expected item/page counts and hashes |
| NT-004 | P0 | CI | NP-083 | Import hostile ZIP: traversal names, duplicate entries, absolute paths | All unsafe entries rejected without outside-root writes |
| NT-005 | P0 | CI | NP-083 | Import ZIP bomb and oversized manifest/assets | Bounded failure; no runaway inflation or temp file leaks |
| NT-006 | P0 | CI | NP-054 | Feed malformed/encrypted PDF to parser with wrong/missing password | Predictable error path, no partial overwrite |
| NT-007 | P0 | CI | NP-081 | Check custom export output copies in isolated test fixture | Output bytes valid; no success before actual completed write |
| NT-008 | P1 | CI | NP-032 | Save, reload, delete and max out three pen presets per tool | Roundtrip identical; overflow bounded; old settings load |
| NT-009 | P1 | CI | NP-072 | Parse valid and invalid page ranges including 1,3-5 and overflow | Only valid normalized indices exported |

## Library, first-run and creation

| Test | Priority | Environment | NP IDs | Steps | Pass condition |
|---|---|---|---|---|---|
| NT-010 | P1 | Phone | NP-001,NP-002 | Launch after fresh install without internet/account | Guest can create and save note; no forced login |
| NT-011 | P1 | Phone | NP-003,NP-004 | Use Home quick Create/Quick Scratch when implemented | New writable note opens, name/unsaved state sensible |
| NT-012 | P1 | Phone + Tablet | NP-005,NP-009 | Create Standard Note and Infinite Canvas | Both open with correctly sized writable surface |
| NT-013 | P1 | Phone + Tablet | NP-009–NP-012 | Create A4 ruled and custom landscape pages; insert template | Preview equals actual paper and export size |
| NT-014 | P1 | Phone | NP-013,NP-014 | Create 15 notes; search, sort and grid/list toggle | Correct results; view toggle retains filter |
| NT-015 | P2 | Phone | NP-015,NP-016 | Star/tag/color-filter note when implemented | Tags/favorites persisted and filter accurately |
| NT-016 | P0 | Phone | NP-017,NP-018 | Move folder; delete/trash; restore; bulk-select two notes | Both notes survive with assets and expected names |
| NT-017 | P1 | Phone | NP-008,NP-007 | Import images/media via SAF and open unsupported file | Supported media usable; unsupported file rejected helpfully |
| NT-018 | P2 | Tablet | NP-019 | Open empty library/folder and filtered empty state | Create/import and clear-filter remain discoverable |

## Stylus, pointer ownership and pen tools

| Test | Priority | Environment | NP IDs | Steps | Pass condition |
|---|---|---|---|---|---|
| NT-019 | P0 | S Pen tablet | NP-020–NP-022 | Write with palm resting on screen; add and lift finger mid-stroke | No scrolling, phantom dots, missing segments or broken pen-up |
| NT-020 | P0 | S Pen tablet | NP-022 | Write in right/low scrollbar band with stylus tip and eraser tip | Scrollbar never steals pen; ink/eraser works |
| NT-021 | P0 | S Pen tablet | NP-022 | Finger drags scrollbar, then stylus begins ink over it | Scrollbar releases, pen owns remaining gesture |
| NT-022 | P0 | S Pen tablet | NP-022 | Finger alone drags vertical and horizontal scrollbar | Scrollbar still scrolls correctly, no ink |
| NT-023 | P0 | S Pen tablet | NP-023–NP-025 | Write low/high pressure stroke and lift/restart quickly | Smooth width and no lost last frame |
| NT-024 | P1 | S Pen tablet | NP-024 | Tilt stylus 0–60 degrees on supported device | No crashes; tilt behavior matches explicit user setting if implemented |
| NT-025 | P0 | S Pen tablet | NP-026–NP-028 | Enable stylus-only mode; finger pan, two-finger pinch and button erase | Finger navigates; pen draws; button toggles exact intended tool |
| NT-026 | P1 | Phone + Tablet | NP-029 | Switch PDF Read → Annotate → Eraser → Read → Annotate | Pan and previous editing tool restored; current page retained |
| NT-027 | P1 | S Pen tablet | NP-030–NP-031 | Draw 20 strokes with each pen type and highlighter | Correct pressure/opacity, no accidental source-PDF changes |
| NT-028 | P1 | Phone + Tablet | NP-032 | Save 3 presets, delete middle, save replacement and relaunch | Slot order/color/width/pressure intact on both editors |
| NT-029 | P2 | Tablet | NP-033–NP-035 | Drag floating toolbar, collapse, expand, rotate device | Within bounds, no canvas ink generated by toolbar drag |
| NT-030 | P2 | Phone | NP-036–NP-038 | Draw shape, edit text/image/audio, open link | Actions undo, save and reopen without lost assets |

## Point/Stroke eraser, selection, layers and undo

| Test | Priority | Environment | NP IDs | Steps | Pass condition |
|---|---|---|---|---|---|
| NT-031 | P0 | S Pen tablet | NP-039 | Erase middle of long 2-point pen stroke in AREA mode | Both ends remain as independently editable segments |
| NT-032 | P0 | S Pen tablet | NP-040 | Stroke erase across 3 lines in one gesture | Intersected whole strokes removed, others unchanged |
| NT-033 | P1 | S Pen tablet | NP-041,NP-048 | Select two objects with lasso; delete and undo once | Only selected objects removed, restored intact |
| NT-034 | P1 | S Pen tablet | NP-042 | Change eraser size 4/24/80 and zoom 25–400% | Cursor ring and erased width match selected size |
| NT-035 | P2 | S Pen tablet | NP-043 | Toggle pressure eraser size if implemented | Pressure variation changes only eraser radius |
| NT-036 | P2 | S Pen tablet | NP-044–NP-045 | Mix pen/highlighter/tape and erase with type filters | Only opted-in object kind removed |
| NT-037 | P1 | Phone + Tablet | NP-046 | Erase PDF highlight with markups toggle on/off | Markup changes only when enabled, PDF text preserved |
| NT-038 | P0 | S Pen tablet | NP-047 | Rapidly erase dense ink on 30-page PDF | No flicker, full-page clear, or out-of-order frame |
| NT-039 | P0 | S Pen tablet | NP-048–NP-049 | Cross same stroke 3x; Undo once; Redo; new edit | Correct fragments and redo branch clears after new edit |
| NT-040 | P1 | Phone + Tablet | NP-049 | Undo on left/right split pane with distinct inks | Only active pane edited |
| NT-041 | P2 | Tablet | NP-050 | Hide/lock named layer when implemented and erase over it | Locked/hidden items unchanged; restore preserves state |
| NT-042 | P2 | Phone | NP-051–NP-052 | Resize/copy/lasso selection and navigate with TalkBack | No object ID drift; controls spoken and accessible |

## PDF, pages, transforms and split view

| Test | Priority | Environment | NP IDs | Steps | Pass condition |
|---|---|---|---|---|---|
| NT-043 | P0 | Tablet | NP-053–NP-054, NP-058 | Import local/cloud PDFs; batch good/malformed/disconnected PDFs; cancel/retry import; insert malformed/short-page-geometry PDF into existing note, then retry valid PDF; inspect 200-page and 5,000-page PDFs and reject 5,001-page source; append to a 4,999-page PDF once and then attempt another append; reject empty or 5,001-page extraction | Original PDF and notes unchanged on failure; combined page budget never exceeded; no missing/extra geometry or excessive PDFBox work; invalid pages never inserted as blank pages; no staged/partial join files remain; valid PDF inserts, saves and reopens |
| NT-044 | P0 | Tablet | NP-055 | Anchor ink dot at image landmark; zoom 50→400→100% | Ink remains on landmark, including export |
| NT-045 | P0 | Tablet | NP-055–NP-056 | Repeat with 0/90/180/270 rotation and crop/media boxes | Overlay and eraser hit-test use same page transform |
| NT-046 | P1 | Tablet | NP-057–NP-060 | Insert/duplicate/move/delete selected pages including cover | Correct order, undo, thumbnails and source page pointers |
| NT-047 | P2 | Tablet | NP-061 | Open annotations tab/list when implemented and jump to item | Correct highlight/page selected |
| NT-048 | P1 | Phone + Tablet | NP-062, NP-085 | Extract a single page and multiple non-adjacent pages to `.xnote`; include editable ink, image/audio, templates, bookmarks and PDF backgrounds; close original and reopen extracted note; cancel immediately after Extract/Move starts and midway through large image/audio asset copy; retry Move | Original unchanged on Extract or failed/cancelled Move; no duplicate destination or abandoned partial note; new note reopens with editable strokes and independent embedded assets in correct page order |
| NT-049 | P1 | Tablet | NP-063–NP-065 | Use bookmarks, outline, PDF text search and markups | Targets highlighted and persisted after reorder |
| NT-050 | P1 | Tablet | NP-066 | Switch Read/Annotate and fit page width/full page | No re-open/reset of PDF viewport or incomplete stroke |
| NT-051 | P1 | Tablet | NP-067 | Two notes in split vertical and horizontal orientation | Correct pen pane focus, independent undo and saved positions |
| NT-052 | P2 | Tablet | NP-068–NP-069 | Adjust page margins and jump via internal hyperlinks | Page and link anchors remain correct |
| NT-053 | P0 | Tablet | NP-070 | Scroll/annotate 200-page scanned+vector PDF under memory pressure | No repeated full-image rebuild, OOM, or UI freeze |

## Export, persistence, backup, crash and privacy

| Test | Priority | Environment | NP IDs | Steps | Pass condition |
|---|---|---|---|---|---|
| NT-054 | P0 | Phone | NP-071 | Save .xnote and .xcanvas with ink, image, text, audio | Reopen identical editable content |
| NT-055 | P1 | Phone | NP-072 | Export This/Selected/All/range 1,3-5 to PDF and PNG; open each PNG in gallery | Files have exact chosen pages, valid MIME, and no blank/corrupt images |
| NT-056 | P1 | Phone | NP-073 | Read export hint then export and reopen in third-party PDF viewer | User-facing copy accurately distinguishes native editability |
| NT-057 | P1 | Tablet | NP-074 | Create PDF with independent annotation objects when supported | Acrobat/Xodo can select/edit annotations individually |
| NT-058 | P2 | Phone | NP-075 | Export JPEG at two quality settings when implemented | Actual JPEG bytes, color and filenames correct |
| NT-059 | P2 | Phone | NP-076–NP-080 | Toggle background, bookmark, item, margin and watermark settings | Each output changes as claimed; native document untouched |
| NT-060 | P0 | Phone | NP-081 | Cancel SAF picker; simulate null output, slow cloud provider, revoked URI, failed multi-PNG write, and existing or unknown-size targets for native copy/PDF/PNG and ZIP | No false success; image counts accurate; partial output removed or warned; occupied targets stay byte-identical, unreadable targets are denied without write/delete |
| NT-061 | P0 | Phone | NP-082 | Cancel export during 200-page render, repeat then save | No leaking temp files, correct final bytes |
| NT-062 | P0 | Phone | NP-083 | Import malformed ZIP, duplicates, traversal and oversized assets | Controlled error, no data escape or unrelated note damage |
| NT-063 | P0 | Phone | NP-084 | Force-stop during Save As, simulate low storage | Old valid note stays valid, destination not falsely reported saved |
| NT-064 | P1 | Phone | NP-085 | Extract 20 selected pages to a new native note, cancel a large Merge before/while writing, then retry | All new IDs/assets independent and editable; Cancel creates no false success, source note remains unchanged and no incomplete new file remains |
| NT-065 | P2 | Phone | NP-086–NP-087 | Restore older note version and settings backup when implemented | Preview and confirmation; unrelated notes unchanged |
| NT-066 | P3 | Phone offline | NP-088 | Disable network when optional cloud sync enabled | Offline editing remains possible; queued action explicit |

## Usability, accessibility and optional features

| Test | Priority | Environment | NP IDs | Steps | Pass condition |
|---|---|---|---|---|---|
| NT-067 | P1 | Phone + Tablet | NP-089 | Measure effective action hit areas, including split divider | Tap/drag controls ≥48dp or defensibly expanded |
| NT-068 | P1 | TalkBack | NP-090 | Navigate all toolbar icon buttons and saved pen presets | Name/action/selected state spoken, correct focus order |
| NT-069 | P1 | Phone + Tablet | NP-091 | Rotate during editor, split view and modal open | No clipped Save/export, preserved viewport |
| NT-070 | P1 | Phone | NP-092 | Set font scale to 1.3 and 2.0; open export/pen/settings | No unreachable destructive/confirm action |
| NT-071 | P2 | Tablet | NP-093 | Find screenshot, hyperlink and layer controls in long settings list | Search/grouping navigable and persisted if implemented |
| NT-072 | P2 | Phone | NP-094 | Switch to reading layout while PDF open | More space; navigation remains discoverable |
| NT-073 | P2 | Phone + Tablet | NP-095 | Light, Dark, OLED and dynamic color screenshot comparison | Text/icons contrast measured and PDF colors unchanged |
| NT-074 | P2 | Phone | NP-096 | Disable animation scale and interrupt transitions mid-gesture | No stuck overlay, dropped pen or unintended repeated action |
| NT-075 | P2 | Phone | NP-097 | Dismiss onboarding/gesture help and reopen editor | Help optional, never blocks writing |
| NT-076 | P1 | TalkBack | NP-098–NP-100 | Use split divider, pen slider and export cancel/error | Values and recovery announced; no color-only status |
| NT-077 | P2 | Tablet offline | NP-101 | If OCR implemented, scan PDF page at 90° and search recognized word | Local index and correct rotated text hit; no upload |
| NT-078 | P3 | S Pen tablet offline | NP-102 | If handwriting recognition implemented, write English/Hindi test corpus | Text recognized with measured quality and preserved ink |
| NT-079 | P3 | Phone offline | NP-103–NP-108 | Confirm optional network/identity/AI/lock features absent or opt-in | No silent account registration or network upload |

## Evidence recording format

For every case, record `NT-### / date / source SHA / CI run URL or device model + Android version / fixture name and SHA / exact actions / observed vs expected / result (pass, fail, blocked) / screenshots/log/output path` in [PROGRESS.md](PROGRESS.md). A test remains pending unless this information exists.

For style/toolbar checks, capture phone 320/360dp and tablet portrait/landscape, light/dark/OLED and font scale; for file tests verify exported bytes in another reader and save/reopen afterward. Use destructive cases **only** on disposable documents. Preserve `.xnote`, `.xcanvas`, `.xtemplate` format compatibility and keep original imported PDFs untouched.

**Exit gate:** No P0 regression unverified; every NP feature claimed complete maps to at least one recorded test result and exact CI/device evidence. Optional AI/cloud items are not release blockers for the offline-first core.
