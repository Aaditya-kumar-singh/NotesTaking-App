# Canvas and PDF Research - Apps, GitHub Source Map, Reddit

Research date: 2026-10-28
Use: product inspiration and technical investigation, NOT blanket permission to copy code or visual identity.
Scope: Android handwriting, PDFs, inserted writing pages, brush/eraser presets, stylus latency.

## How to read
- Evidence = source-backed observed behavior or published documentation.
- Recommendation = proposed design for NotesTaking App, not a fact about an external product.
- Verify code license, individual file headers, dependency notices and project status **again at integration time**.
- Preferred approach: use user flows as inspiration and implement independently with our existing MIT-derived Kotlin architecture. GPL/AGPL and noncommercial/no-derivatives code must not be pasted into this app.

## 1. Comparison and useful ideas

| Reference | Link | Source-grounded focus | Lessons for NotesTaking App |
|---|---|---|---|
| Samsung Notes official help | https://www.samsung.com/us/support/answer/ANS10004548/ | Individual vs infinite notes, PDF import, templates, edit controls | Default PDF-backed notes to paged layout; keep blank sheet insertion discoverable |
| Samsung Notes pen help | https://www.samsung.com/in/support/mobile-devices/how-to-use-samsung-notes/ | Favorite pens, type, width, color | Persistent 4-8 pen presets; 1-tap switching |
| Goodnotes Android official guide | https://support.goodnotes.com/hc/en-us/articles/7378598719759-Get-Started-With-Goodnotes-for-Android-Windows-and-Web | Sidebar Add page, Add Before/After from a thumbnail, tools | Obvious Add Page affordance and page manager; minimal taps |
| Goodnotes imports | https://support.goodnotes.com/hc/en-us/articles/7353717816463-Import-files-into-Goodnotes | PDF import into existing document | Make Insert PDF adjacent to Add Page |
| Flexcil | https://www.flexcil.com/ | 2/4-page view, PDF tools, vector ink, sticky notes, masking, audio | Student study layout; compare latency and reading workflows |
| Xournal++ | https://github.com/xournalpp/xournalpp and https://xournalpp.github.io/guide/pdfs/ | PDF background, added journal sheets, pen/highlighter/eraser, page sidebar and layers | Explicit PDF background vs editable note-sheet model; avoid lost PDF/ink alignment |
| Linwood Butterfly | https://github.com/LinwoodDev/Butterfly | Infinite workspace, pens, rulers, layers, palettes, local-first | Canvas organization, tool customization and cross-device navigation |
| Saber | https://github.com/saber-notes/saber | Under-text highlighter compositing and no-overlap-darkening; offline-first handwriting | Highlighter rendering correctness and readability |
| Rnote | https://github.com/flxzt/rnote | Pressure styling, selectable shapes, fixed/infinite layouts | Stylus tuning and separable canvas modes |
| NeXopp (Android) | https://github.com/bamonroe/NeXopp | PDF append vs replace, undoable page insertion, blank sheets, grid view, .xopp interoperability | Android-specific proof of similar interactions and file-format pitfalls |
| Obsidian Handwriting plugin | https://github.com/ellimist-afk/handwriting | Pen/eraser presets, PDF overlay, auto-hide toolbar, lasso, sidecar storage | Keep ink overlay editable and avoid PDF content mutation; **do not copy license-incompatible code** |
| Android Jetpack Ink | https://developer.android.com/develop/ui/compose/touch-input/stylus-input/about-ink-api | Low-latency wet and finalized ink APIs | A/B spike, not rewrite |
| Android stylus docs | https://developer.android.com/develop/ui/compose/touch-input/stylus-input/advanced-stylus-features | Pressure, tilt, hover, eraser tip, historical motion, prediction | Input data handling and fallbacks |
| Android large-screen input | https://developer.android.com/develop/ui/compose/touch-input/input-compatibility-on-large-screens | Palm cancellation and historical samples | Input stress scenarios and stylus platform validation |
| Android Ink Kotlin example | https://github.com/hearsilent/InkSample | InProgressStrokesView example with color and pressure | Lightweight isolated prototype; evaluate version/minSdk differences |
| Native Kotlin brush example | https://github.com/alfredang/sketchbookapp_android | Pressure-profiled brushes, layers, texture variants | Read architecture; reimplement uniquely after license review |
| Folio experimental Android | https://folio.allient.io/ | Different pencils/brushes and PDF/EPUB study mode claimed by author | Inspiration only; verify source, device behavior and availability separately |
| inklus | https://github.com/Pedroj-64/inklus | Lasso, pressure-aware handwriting, pen favorites, PDF import | Feature checklist; Flutter architecture not directly transferable |

## 2. Reddit / community-derived pain points (anecdotal, not survey data)

1. A Reddit user asks for **offline PDFs, imported documents, multiple pen styles, tape, adding pages to imported PDFs and folder organization**. It closely matches our feature brief: https://www.reddit.com/r/NoteTaking/comments/1ht8ia8/ (historical single-user request, not a representative frequency estimate).
2. Galaxy Tab discussions of Samsung Notes describe **imported PDFs appearing as a single long page when infinite style is configured**, preventing normal page sorting; fix is to use individual page style: https://www.reddit.com/r/GalaxyTab/comments/13c160b/ and https://www.reddit.com/r/GalaxyTab/comments/15fcnvx/. Design response: PDF opens with a clear per-document page mode, not inherited invisible global behavior.
3. Discussion of note apps emphasizes **editable PDF annotations, handwriting search and custom fonts**: https://www.reddit.com/r/GalaxyTab/comments/18gvwtk/the_best_handwritingnotetaking_apps/. Design response: treat editable export and OCR as explicit future work, not implied by flatten export.
4. Galaxy Tab user compares apps and mentions **finger scroll while writing with pen** as valuable, and two-finger scrolling as annoying: https://www.reddit.com/r/GalaxyTab/comments/1h5k4nb/. Design response: fingers pan; stylus writes; predictable palm rejection and separate pan toggle.
5. New Samsung Notes user reports import-menu discoverability problems after UI updates: https://www.reddit.com/r/GalaxyTab/comments/1t3k94p/where_is_the_add_pdf_option/ (May 2026). Design response: avoid hidden long-press-only PDF import and unexpected pen-selector popups.
6. Recent stylus app developer requests cross-stylus testing (S Pen, USI, MPP) and discusses low-latency pressure/palm handling: https://www.reddit.com/r/stylus/comments/1wz258k/i_made_a_lowlatency_handwriting_app_for_android/ (Oct 2026). Design response: real-hardware matrix.
7. Obsidian plugin release discussions demonstrate interest in **writing/highlighting/erasing/lasso directly on PDFs and notes** and mention a regression in experimental zoom: https://www.reddit.com/r/ObsidianMD/comments/1w4hl75/onenote_handwriting_in_obsidian_now_with_pdf/ and https://www.reddit.com/r/ObsidianMD/comments/1wg8ayq/infinite_canvas_for_handwriting_features_update/ (Sep 2026). Design response: test coordinate mapping and undo under zoom and resizing.
8. Flexcil discussion praises **bookmarks/outlines and offline backups** while expressing concern about update stability: https://www.reddit.com/r/Flexcil/comments/1t78x5q/flexcil_is_the_best/ (May 2026). Design response: preserve stable document compatibility and backup behavior.

Do not quantify community sentiment as percentages from these posts, and do not assume every feature mentioned remains in an app's newest version. Community complaints are supporting evidence, not bug reproduction.

## 3. License and source integration guidance

| Repo | Reported license in repository/source | Suitability |
|---|---|---|
| Xournal++ | GPL-2.0 (see repo LICENSE) | Study concepts and algorithms; do not paste code into MIT project without legal/license review |
| NeXopp | GPL-2.0-or-later (see repo README) | Architectural inspiration only unless license compatibility resolved |
| Linwood Butterfly | AGPL-3.0 (assets/docs also separately licensed) | UI/workflow inspiration only; no copy |
| Rnote | GPL-3.0-or-later | Study feature design, not directly transplant code |
| Saber | Check actual repository LICENSE at use time; GPL family reported by ecosystem | Treat as copyleft pending verification; no direct copy |
| Obsidian Handwriting plugin | CC BY-NC-ND 4.0 per README | **Do not copy or adapt code/assets**; use ideas only |
| Android documentation / Jetpack Ink library | Verify official individual artifact licensing and API compatibility | Preferred native integration path if benchmark supports use |
| Unverified smaller projects | Must inspect LICENSE and activity before use | No copying, especially assets and brush textures |

Even public GitHub repos may not license all files equally. App names/logos/screenshots are used here as research references, not assets to ship.

## Android native branding & component system research (2026-10-28)

Official Android sources support adaptive launcher icon layers, 108dp outer composition, safe inner artwork, a dedicated monochrome layer and themed launcher previews: https://developer.android.com/develop/ui/compose/system/icon_design_adaptive. Material 3 Compose provides color roles, typography, shapes and dynamic wallpaper colors (https://developer.android.com/develop/ui/compose/designsystems/material3), and custom Compose themes may extend Material rather than replace it (https://developer.android.com/develop/ui/compose/designsystems/custom). Android 16 behavior guidance: https://developer.android.com/about/versions/16/behavior-changes-all. The plan explicitly reuses this app's existing Palette, MaterialColors, dynamic color, corner settings, NotesTakingIcons and styled AlertDialog rather than installing a heavy new design library. Complete asset/interaction system: **VISUAL_BRANDING_THEME_AND_INTERACTIONS_PLAN.md**; 33-feature code-backed coverage audit: **BRAND_UI_AUDIT_MATRIX.md**; issues CV-076–CV-100. No images/assets have been generated; three original icon routes are for later design approval.

## Samsung Notes-style eraser UX (official reference)

Samsung Notes documents a **Stroke eraser** (whole touched line) and **Area eraser** (only ink touched), a size slider and, in some variants, **Erase highlighter only**. It also documents temporary S Pen button erasing and restoring the prior tool. Official support: https://www.samsung.com/us/support/answer/ANS10002888/ and https://www.samsung.com/us/support/answer/ANS10003634/. This is directly relevant to the user's complaint. Our source already has both eraser modes but defaults to Stroke and clips only stored samples; it needs a real geometry-aware precision sweep for excellent quality. Full file-by-file audit, 15 new development tickets CV-061–CV-075 and tests: **SAMSUNG_NOTES_STYLE_ERASER_PLAN.md**. Samsung is UX inspiration, not a code/asset source; its app is proprietary. No extra eraser dependency is needed for the core fix.

## 4. Best-rated reusable OCR/native modules — follow-up research

The 2026-10-28 deeper comparison, current GitHub counts, model/licensing caveats and detailed implementation plan are in **OCR_LIBRARY_RESEARCH_AND_INTEGRATION_PLAN.md**. The primary decision is **reuse-first, not popularity-first**:

| Problem | Candidate and source | Engineering choice |
|---|---|---|
| Existing selectable PDF | Internal PdfiumDocument and core/search | Reuse current code; do not run OCR |
| Scanned PDF English/Hindi | ML Kit Text Recognition v2 (https://developers.google.com/ml-kit/vision/text-recognition/v2/android) | Fastest native Android Kotlin pilot; Google SDK terms and bundled model size need review |
| Fully open-source scanned PDF OCR | Tesseract4Android (https://github.com/adaptech-cz/Tesseract4Android) + https://github.com/tesseract-ocr/tessdata_fast | Mature Apache-2.0 Android wrapper; benchmark Hindi+English |
| Potential higher-quality advanced OCR | PaddleOCR (https://github.com/PaddlePaddle/PaddleOCR) | Popular Apache-2.0 models; Android runtime/model adaptation required |
| Lightweight model runner experiments | RapidOCR (https://github.com/RapidAI/RapidOCR) and https://github.com/RapidAI/RapidOcrAndroidOnnx | Main repo active, Android ONNX demo older; not a drop-in library |
| Native vector handwriting recognition | ML Kit Digital Ink (https://developers.google.com/ml-kit/vision/digital-ink-recognition/android) | Hindi and English supported; first-time language model download, then offline |
| OCR camera/image cropping | Existing platform/ImageCropper; optional https://github.com/CanHub/Android-Image-Cropper | Existing cropper first; avoid duplicate dependencies |

GitHub stars are social popularity, **not OCR accuracy, production readiness, or Play Store ratings**. Vendor model benchmark accuracy is not comparable across different testing datasets. All candidates are **research only**, with zero new app dependencies installed.

## 5. Research conclusions

- **Top 1:** Make adding a writing sheet to a PDF obvious, keeping source pages and annotations aligned.
- **Top 2:** Save and switch favorite natural pens without repeated menus; pencil/gel/fountain styles have different physical behaviors.
- **Top 3:** Maintain correct undo/save/restore and exports for mixed PDF/note documents; safety outranks adding dozens of brushes.
- **Top 4:** Keep stylus and finger roles predictable; aim for low-latency writing and test real tablet devices.
- **Top 5:** Add student productivity extras (multi-page study, bookmarks, tape/flashcards, search) only after core reliability.

No external code has been imported into NotesTaking App by this research.
