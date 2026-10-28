# OCR, Handwriting Recognition & Reuse-First Library Research

**Date:** 2026-10-28
**Project:** NotesTaking App (native Kotlin + Compose + PDFium), current local Git branch `master`
**Status:** RESEARCH / ARCHITECTURE DECISION PROPOSAL. No dependency installed, no OCR implemented.
**Purpose:** Choose existing libraries by suitability, license, genuine repository activity, user value and benchmarks—not GitHub stars alone.
**Related:** `MASTER_CANVAS_UPGRADE_PLAN.md`, `RESEARCH_AND_SOURCE_MAP.md`, `FEATURE_BACKLOG.md`.

## 1. Executive recommendation

- **Stage 0 (reuse existing):** For PDFs with real/selectable text, use the app's **existing PDFium text extraction/search**, not OCR. `platform/PdfiumDocument.kt:93-110`, `platform/PdfSource.kt:57-65` and `core/search/` already cover relevant operations. Keep all OCR out of the live stylus rendering thread.
- **Stage 1 (printed scans, best first integration):** Prototype **ML Kit Text Recognition v2, bundled Latin and Devanagari**. It directly supports Kotlin/Android API 21+ and both target scripts. Bundled models work from first launch without a Play Services model download, but the SDK is **not an open-source OCR engine** and Google SDK terms must be reviewed. Avoid a network-dependent unbundled option for strict first-use offline support.
- **Stage 1 (best fully open-source Android fallback):** Prototype **Tesseract4Android** + `eng` and `hin` `tessdata_fast`, Apache-2.0 engine/wrapper; prefer a maintained, pinned source/tag and verified binary. It has a mature Android JNI/Java API and a sample app. Strict full-offline/F-Droid/Google-free distributions should select this by default, pending accuracy tests.
- **Stage 2 (advanced model benchmark, not automatic integration):** Compare **PaddleOCR PP-OCRv5 mobile English + Devanagari models** to ML Kit and Tesseract using our own dataset. Apache-2.0; strong ecosystem and documented Hindi recognition model. Mobile inference and build toolchain (Paddle Lite or compatible runtime) need a reproducible Android proof-of-concept, and latest models must be checked for runtime compatibility.
- **Stage 2 alternative:** **RapidOCR** (Apache-2.0) provides ONNX/MNN-related inference components, but the Android path is a separate, smaller and older Android ONNX demo. The main `RapidOCR/android/README.md` forwards to `RapidOcrAndroidOnnx`; the main repo is not a plug-and-play Android AAR. Evaluate only in an isolated spike, with modern Gradle, Android API, ABI and model support tested.
- **Stage 2 (existing stylus vectors, not image OCR):** Prototype **ML Kit Digital Ink Recognition** in a separate module. It accepts stroke coordinates/timing, handles English (`en`) and Hindi (`hi`), and runs offline **after** per-language model download (the model cannot be assumed available on a fresh offline installation). Google's Android sample is Apache 2.0, **the SDK itself is not open-source**. Keep original strokes editable and overlay/suggest text rather than destructively replacing ink.
- **Deferred:** EasyOCR, docTR and large VLM OCR. Good Python/server projects, but poor fit for an offline-only, small native Android APK without a separately justified deployment strategy.

**Go/no-go:** No OCR engine is selected as a shipped dependency yet. Make a 2-engine Kotlin device spike and evaluate correctness, latency, memory, ABI, APK/AAB size, licensing and model availability. Do not call a project 'best accuracy' from stars or marketing percentages.

## 2. Live public GitHub metadata (snapshot 2026-10-28)

Fetched from GitHub repository metadata with the connected GitHub tool. Star counts are popularity indicators, **not** recognition accuracy measurements or Google Play user ratings. `pushed_at` is the public repository API's push timestamp; it can include non-release activity.

| Project | GitHub | Stars | Last push observed (UTC) | Code license | Notes |
|---|---|---:|---|---|---|
| PaddleOCR | https://github.com/PaddlePaddle/PaddleOCR | 91,846 | 2026-09-16 | Apache-2.0 | Most popular among these and native-mobile docs exist, but not a drop-in Android SDK |
| Tesseract engine | https://github.com/tesseract-ocr/tesseract | 77,212 | 2026-10-28 | Apache-2.0 | Mature engine; integrate Android wrapper, don't replace PDFium |
| EasyOCR | https://github.com/JaidedAI/EasyOCR | 30,116 | 2025-12-05 | Apache-2.0 | Python/PyTorch, heavyweight on-device deployment |
| RapidOCR | https://github.com/RapidAI/RapidOCR | 8,361 | 2026-10-28 | Apache-2.0 | Actively developed parent; mobile demo separately maintained |
| Tesseract4Android | https://github.com/adaptech-cz/Tesseract4Android | 951 | 2026-02-05 | Apache-2.0 | Direct Android AAR/JNI library, maintained wrapper |
| Rapid OCR Android ONNX demo | https://github.com/RapidAI/RapidOcrAndroidOnnx | 118 | 2025-04-17 | Apache-2.0 | Older model/runtime versions; modernize before any adoption |
| Rapid OCR Android Compose demo | https://github.com/RapidAI/RapidOcrAndroidOnnxCompose | 34 | 2025-03-25 | Apache-2.0 | Architecture inspiration, not current production proof |
| ONNX Runtime | https://github.com/microsoft/onnxruntime | 22,184 | 2026-10-28 | MIT | Runtime only; needs models, preprocessing/postprocessing, OCR integration |
| CanHub Image Cropper | https://github.com/CanHub/Android-Image-Cropper | 1,505 | 2026-04-28 | Apache-2.0 | Potential OCR image crop UI; compare against existing cropper first |
| Google ML Kit samples | https://github.com/googlesamples/mlkit | approx. 4.3K (public page) | not independently recorded | Apache-2.0 sample source | **Do not mistake sample repo license for proprietary Google SDK licensing** |

Counts will change. Do not update these as if they are official app-store ratings.

## 3. Task-specific comparison: what to use where

| Scenario | Primary recommendation | FOSS-only alternative | Why |
|---|---|---|---|
| Selectable PDF text | Existing PDFium text/search (zero new dependency) | Same | OCR only adds errors and CPU cost |
| Scanned English PDF/image | ML Kit Text Recognition v2 Latin bundled | Tesseract4Android eng fast | Android-native Kotlin integration vs entirely open engine |
| Scanned Hindi/Devanagari PDF/image | ML Kit Text Recognition v2 Devanagari bundled | Tesseract4Android hin; PaddleOCR v5 Devanagari in benchmark | Native support vs open models; measure real fonts/diacritics |
| English + Hindi mixed scans | Script-aware ML Kit routing or region segmentation, evaluate outputs | Tesseract eng+hin and PaddleOCR v5 model mix | Don't assume simultaneous scripts work perfectly without tests |
| Stylus-drawn handwriting | ML Kit Digital Ink (model download then offline) | Research alternative stroke recognizers and model licenses | Coordinates and timestamps contain information raster OCR loses |
| Photo of old handwriting | Printed OCR baseline first; add specialized offline HTR after test | PaddleOCR English rec model has some handwritten text improvements | Digital Ink API cannot retroactively recover stroke timing from photo |
| Tables, equations, layouts | Future constrained experiment | PaddleOCR document models/server optional | Very different workload; avoid huge mobile VLM |
| OCR search | Integrate output with existing core/search and PageText/PdfSource semantics | Same | Searchable note text should have source page positions and freshness |

### Constraints verified in this repository
- Android minSdk 26, targetSdk 36, Kotlin 2.2.21, Java 17, native ABIs arm64-v8a/armeabi-v7a/x86_64 (see `app/build.gradle.kts`).
- App already bundles large native PDFium and Tree-sitter code. New OCR native dependencies may increase APK size and build/ABI risk. Do not assume all proposed artifacts support all three ABIs.
- Existing `PdfiumDocument.pageText()` includes character boxes; `PdfiumDocument.searchText()` returns searchable text without rendering images.
- `ui/Editor.kt:3323` and `core/search/SearchSession.kt` already implement document search. New OCR should feed a compatible page-indexed text source rather than building a disconnected search UI.

## 4. Implementation architecture and data policy

Create an **internal app-level interface** with `RecognitionRequest(pageId, sourcePdfPageIndex, rotation, languageHint, image, cancellation)` and `RecognitionResult(text, lines, polygons, confidence?, modelId, version)`. Avoid engine-specific objects in documents, UI and tests; build adapters:
- `PdfTextAdapter`: existing PDFium text route (not actually OCR).
- `MlKitPrintedAdapter`: default Latin/Devanagari image OCR, optional, isolated.
- `TesseractPrintedAdapter`: FOSS fallback, optional, process singleton or thread-isolated per engine requirements.
- `PaddlePrintedAdapter`: R&D branch only, not a shipping dependency until benchmarks pass.
- `MlKitDigitalInkAdapter`: separate API accepting ordered ink strokes/timestamps and writing-area metadata; never pass images to it.

**Recognition policy**
1. On page open/search index request, get PDF text first. Determine whether an image-only page lacks *meaningful* visible/searchable text using text length, Unicode validity and page coverage; simple zero-length heuristic alone can fail on mixed/vector PDFs.
2. Ask to "Make scanned pages searchable" or offer user opt-in; avoid silently processing every page in a 300-page document.
3. Render at controlled scale from PDFium via off-main-thread worker, with strict max pixels, RAM/cancel budget and single-page/bounded concurrency (e.g. one page at a time on low-end devices).
4. Preprocess orientation and contrast *without* permanent source alteration. Apply recognition to rendered **original PDF colors**, not user-inverted reading mode or annotation overlay unless OCR-on-annotations is explicitly selected.
5. Keep regions in canonical page coordinate system (PDF points / logical page transform); convert engine bitmap pixel polygons using a tested rotation, crop, zoom and matrix inverse. PDF source page index is not the same as note logical index after inserted sheets.
6. Return text, boxes, language, engine/model version and uncertainty. Present 'Copy', 'Search', 'Correct text'; never present low-confidence guesses as verified.
7. Save OCR **metadata separately** from document strokes and original PDF, with document content hash/mtime + source page ID + engine/model version + OCR mode. Respect document rename, import, reordering and deletion; invalidate when source content changes.
8. For FTS indexing use the app's existing `core/search` interface first; evaluate Room/SQLite FTS only if cross-document indexing or large corpora demand it. Check Hindi normalization and Unicode indexing; no network.
9. Users may remove OCR text indexes without losing their notes; stored ink and PDFs remain authoritative. No automatic uploading of PDFs, screenshots, or writing.
10. Exporting 'searchable PDF' is a **separate feature**: evaluate adding an invisible text layer with correct positions/encoding. Merely finding words in an app index does not turn an exported PDF searchable. Do not claim searchable PDF export until Acrobat/Android/desktop validation passes.

### ML Kit printed integration (candidate, *not installed*)
Official Android guide: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
Supported language guide: https://developers.google.com/ml-kit/vision/text-recognition/v2/languages
- Bundled libraries are named `com.google.mlkit:text-recognition` (Latin) and `com.google.mlkit:text-recognition-devanagari`, as opposed to `com.google.android.gms:play-services-mlkit-...` unbundled.
- Official docs estimate roughly 4 MB application-size increase **per script per architecture** for bundled variant; measure actual APK/AAB impact on our three-ABI release. Use the current compatible version from Google's guide at implementation time; do not hardcode guesses here.
- Google's text recognition engine works on images, not native vector ink samples.

### Tesseract4Android (candidate, *not installed*)
Docs and repo: https://github.com/adaptech-cz/Tesseract4Android
Official trained data: https://github.com/tesseract-ocr/tessdata_fast and https://github.com/tesseract-ocr/tessdata_best
Docs: https://tesseract-ocr.github.io/tessdoc/Data-Files
- README documents Android API 21+, JitPack/JNI AAR and `TessBaseAPI`, an example at version 4.9.0 at research time. **Verify current latest tag, JitPack availability, SHA and transitive dependency licenses before pinning**; avoid floating versions or unverified artifacts.
- It requires `eng.traineddata`, `hin.traineddata` under a readable app-private `tessdata/` directory. `tessdata_fast` emphasizes speed; `tessdata_best` emphasizes accuracy at additional cost.
- `TessBaseAPI` is not thread-safe. Serialize calls or use one per worker; isolate lifetime/cancellation. There is no automatic guarantee that OCR respects page order/layout.

### PaddleOCR (candidate spike, *not installed*)
Repo: https://github.com/PaddlePaddle/PaddleOCR
Official mobile tutorial: https://github.com/PaddlePaddle/PaddleOCR/blob/main/deploy/lite/readme.md
Models: https://www.paddleocr.ai/latest/en/version3.x/module_usage/text_recognition.html
- English model `en_PP-OCRv5_mobile_rec` and Hindi/Devanagari `devanagari_PP-OCRv5_mobile_rec` documented at about **7.5MB recognition model each** (not full detector/recognizer runtime size).
- Official reported accuracy 85.25% English and 84.96% Devanagari **on their respective test conditions**, NOT comparable to other engines without identical evaluation datasets and preprocessing. These are not guarantees for our users.
- `deploy/lite` mobile tutorial focuses older Chinese/English model examples. Latest recognition models need explicit compatibility validation for chosen runtime, detector, recognizer and pre/postprocessing. Do not present a 7.5MB model as the total app-size cost.
- Prefer separate proof-of-concept with Android 26/36, all ABIs, JNI conflict checks, and CPU benchmark before integrating into the main app.

### RapidOCR (candidate spike, *not installed*)
Main: https://github.com/RapidAI/RapidOCR
Android reference: https://github.com/RapidAI/RapidOcrAndroidOnnx
Compose reference: https://github.com/RapidAI/RapidOcrAndroidOnnxCompose
- Main repo `android/README.md` forwards to the standalone Android ONNX demo; as of 2026-10-28 the latter's README emphasizes 2023-era models/runtime and older Gradle. The parent RapidOCR's newer activity does not automatically indicate its Android demo is up to date.
- Validate Android ABI, image processing (OpenCV), shipped .so license/size, inference runtime version, model paths, Hindi language support and current releases before adoption.
- ONNX Runtime https://github.com/microsoft/onnxruntime is MIT, but it is only a model execution runtime and not an OCR recognition model by itself.

### Digital ink (candidate spike, *not installed*)
Docs: https://developers.google.com/ml-kit/vision/digital-ink-recognition/android
Languages: https://developers.google.com/ml-kit/vision/digital-ink-recognition/base-models
Sample: https://github.com/googlesamples/mlkit/tree/master/android/digitalink
- Takes ordered strokes with x/y/time (writing area and context help), **not PDF pixels**. The project stores sample positions/pressure but must verify that timing and stroke grouping are available/derivable; coordinate transforms and selection changes matter.
- Official ML Kit supports Hindi (`hi`) and English; models are downloaded on demand. Network is needed for initial model installation; afterward recognition works offline. Provide clear model-availability/download/storage UI and an offline-unavailable state.
- Do not silently send data to cloud or replace handwritten strokes; include confidence/alternatives and a correction UI.
- The GitHub sample is Apache-2.0. Review Google SDK terms independently.

## 5. Reuse-first decision matrix for OTHER canvas features

This avoids adopting a library just because it has a high star count:

| Need | Existing implementation | Reuse candidate | Recommendation |
|---|---|---|---|
| PDF render, text, image boxes | Bundled PDFium + PdfSource/PageTextCache | New PDF viewer library | **Keep current**; replacement would duplicate native binaries and break annotations |
| Ink rendering and stylus engine | `canvas/`, `core/stroke/`, `gl/` | Android Jetpack Ink: https://developer.android.com/develop/ui/compose/touch-input/stylus-input/about-ink-api | Isolated latency/compatibility spike; not a blanket rewrite |
| Image crop before OCR | `platform/ImageCropper.kt`, `ui/CropOverlay.kt` | CanHub Android Image Cropper: https://github.com/CanHub/Android-Image-Cropper (Apache-2.0) | **Keep existing first**; evaluate only if current controls fail tablet/URI cases |
| PDF annotation & page order | `ui/Editor.kt`, `platform/PdfMarkups.kt`, `core/model/Page.kt` | Xournal++/NeXopp as inspiration | Existing code first; GPL repo code cannot be casually pasted into MIT derivative |
| Color picker, palette | `ui/ColorPicker.kt`, `core/tools/InkPalette.kt` | Many Github Compose color libraries | Keep existing; build missing hex/preset UI with Compose rather than extra dependency |
| OCR / handwriting search | No OCR engine, but PDFium text/search exists | Google ML Kit, Tesseract, PaddleOCR | Add a pluggable adapter layer with benchmark gates |
| Recognition ML runtime | No extra model runtime | ONNX Runtime Mobile (MIT) | Add only if chosen OCR model demands it; do not preload a framework for future hypotheticals |

## 6. Acceptance benchmarks (indispensable before choosing a 'best')

**Construct a legally shareable, ground-truthed corpus** from locally created or properly licensed test documents:
- 40 English printed page crops; 40 Hindi/Devanagari printed crops; 20 mixed English+Hindi; 20 low-resolution/rotated/camera photos; 20 tables/columns/handwritten page images; 20 stylus-written snippets for Digital Ink (different pipeline).
- Diverse font sizes, DPI, color/inversion, marginal notes, rotated/mixed-size pages, both lightweight and large PDFs. These sample counts are **target fixture sizes**, not results or a claim about quality.
- Measure normalized character error rate (CER) and word error rate (WER), line-block order, bounding box IoU, hit highlighting accuracy, and Hindi combining-mark errors. Evaluate each engine against the same relevant subset.
- Measure median/p95 time per page, peak PSS/heap, energy proxy, cold first-use model load time, APK/AAB growth, model package size, offline-first run, cancellation reliability, 30/300-page background indexing. Test minimum API 26 and API 36 on phone/tablet and all supported ABIs.
- Test reading mode inversion, page rotations, crop, zoom, inserted blank page order, foreground edits during OCR, source PDF update and undo. No UI stalls, no user document changes and no remote upload.
- Report real observed figures by device, OS, corpus and build revision. Define practical acceptance targets *after* establishing baseline and measuring all contenders.

**Selection criteria (qualitative review, not invented ratings):**
1. Android API 26+ stable integration / reproducible Gradle build.
2. Correct English and Hindi on *our* corpus.
3. Privacy and genuine offline workflow incl. initial model availability.
4. Apache/MIT/FOSS legal compatibility as required for distribution; if proprietary SDK permitted, evaluate its terms.
5. CPU, memory and APK size; long-document cancellation and thermal cost.
6. Maintenance, security advisories and GitHub issue responsiveness.
7. Stars are only a **tie-breaker**, never a quality metric.

**Exit gates:** (A) ML Kit printed vs Tesseract4Android baseline documented; (B) PaddleOCR/RapidOCR separately run on Android if a worthwhile accuracy/size improvement is plausible; (C) Digital Ink English/Hindi offline model workflow proven; (D) no code merged until regression tests, legal/license review and explicit feature acceptance.

## 7. Feature stages and GitHub tickets

- **Phase OCR-0**: CV-051 map existing PDF text extraction and failure cases; CV-052 design recognizer interface + offline consent.
- **Phase OCR-1**: CV-053 ML Kit bundled printed proof-of-concept; CV-054 Tesseract4Android FOSS proof-of-concept; CV-055 benchmark Hindi/English accuracy/perf/ABIs on real devices.
- **Phase OCR-2**: CV-056 text bounding boxes + search index + corrected text UI; CV-057 privacy/offline-model/user controls; CV-058 stroke-based ML Kit Digital Ink experiment.
- **Phase OCR-3**: CV-059 compare PaddleOCR PP-OCRv5 / RapidOCR native device POC; CV-060 searchable PDF export experiment / accessibility reading-order review.
- Existing **CV-047** remains parent research/epic and links to CV-051–CV-060, not an already shipped feature.
- Recommended development sequence: after PDF save/restore safety work (CV-001–005), research and baseline OCR can run in parallel with the pen/page UX roadmap, but merge only after device verification. Do not delay all canvas work for OCR model R&D.

## 8. Source index and caveats

1. Tesseract engine: https://github.com/tesseract-ocr/tesseract
2. Android wrapper README: https://github.com/adaptech-cz/Tesseract4Android
3. Tesseract data speed/accuracy and Hindi models: https://tesseract-ocr.github.io/tessdoc/Data-Files
4. PaddleOCR: https://github.com/PaddlePaddle/PaddleOCR
5. PaddleOCR mobile tutorial: https://github.com/PaddlePaddle/PaddleOCR/blob/main/deploy/lite/readme.md
6. PaddleOCR official recognition models: https://www.paddleocr.ai/latest/en/version3.x/module_usage/text_recognition.html
7. RapidOCR: https://github.com/RapidAI/RapidOCR
8. RapidOCR Android ONNX: https://github.com/RapidAI/RapidOcrAndroidOnnx
9. RapidOCR Android Compose: https://github.com/RapidAI/RapidOcrAndroidOnnxCompose
10. EasyOCR: https://github.com/JaidedAI/EasyOCR
11. docTR: https://github.com/mindee/doctr
12. ML Kit printed Android: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
13. ML Kit supported languages: https://developers.google.com/ml-kit/vision/text-recognition/v2/languages
14. ML Kit Digital Ink: https://developers.google.com/ml-kit/vision/digital-ink-recognition/android
15. ML Kit Digital Ink languages: https://developers.google.com/ml-kit/vision/digital-ink-recognition/base-models
16. ML Kit open-source demo: https://github.com/googlesamples/mlkit
17. Microsoft ONNX Runtime: https://github.com/microsoft/onnxruntime
18. CanHub crop library: https://github.com/CanHub/Android-Image-Cropper

Snapshot/caveat: public repos and SDKs evolve; stars are rounded/retrieved on 2026-10-28. Some performance/accuracy figures are vendor-specific, not an independent apples-to-apples study. No test APK, Android OCR benchmark, model download, license audit of individual assets, third-party dependency integration or remote GitHub push was performed.
