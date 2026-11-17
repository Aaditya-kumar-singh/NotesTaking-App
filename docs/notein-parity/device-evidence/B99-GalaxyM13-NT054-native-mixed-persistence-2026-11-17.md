# NT-054 — B99 Galaxy M13 native .xnote / .xcanvas mixed-content recovery

**Date:** 2026-11-17 (Asia/Kolkata).

**Checklist:** `docs/notein-parity/TEST_PLAN.md` NT-054 (P0, NP-071), [GitHub issue #30](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/30) for the text-recovery/export defect candidate.

**Verdict:** **PARTIAL PASS / NOT VERIFIED**. Both native archives preserve real media bytes and reopen. One text-rendering/export case is unresolved; no editable canvas text workflow was exercised. Testing stopped after the ADB connection changed to `unauthorized`.

## Build and safe test data

- Physical device: Samsung Galaxy M13 (`SM-M135F`), Android 14, ADB serial `RZ8W103G6EL`.
- Installed app: B99 `com.notestaking.app.debug` version `0.1.0-debug`, source `b299cbb8972267eebe103493fa093f7e5cfa6f70`, installed base APK SHA-256 `25eaf748dbe1c2039c66aa383f7ecc42bd9de2aeb3043dbc47f24fcb3b248025`.
- Two deliberately new disposable notes, created within the app: `QA_NT054_MIXED_NOTE_20261117` and `QA_NT054_MIXED_CANVAS_20261117`.
- Synthetic image (not a personal photo): `/sdcard/Download/QA_NT054_TEST_IMAGE_20261117.png`, 7,875 bytes, SHA-256 `ae51c78dd68508643ecc806ca41bf7f4b951a818da9d2c3b1df651f35ae3c5fe`. Contains an obvious QA label and green line.
- Synthetic audio (not recorded from the microphone): `/sdcard/Download/QA_NT054_TEST_AUDIO_20261117.wav`, 64,044 bytes, 2 seconds of 440 Hz mono 16 kHz test tone, SHA-256 `c1f86a3eaaa86f4102fb6d1d7224183ed8bfa3ee7abe52a0bafb284aa106cea6`.
- Both source media hashes matched after ADB push. The app's **Audio from file** action was used; the **Record audio** action was not invoked.

## Real-device observations

| Scenario | Evidence | Classification |
|---|---|---|
| Paged native note (.xnote): create and ink | Created `QA_NT054_MIXED_NOTE_20261117`, drew a green stroke; visible page diff 2,511 pixels and 2,974 green pixels after drawing | **PASS**, smoke |
| Text entry into paged native note | Selected Text, tapped lower on page (approx `x=340,y=1580`) and typed `QA_NT054_TEXT_20261117`. Text was visibly rendered immediately after editing | **PASS** for initial editing |
| Image attachment to .xnote | Selected exact synthetic PNG through Android SAF; labeled image appeared on page | **PASS** |
| Audio attachment to .xnote | Selected exact synthetic WAV through `Audio from file`; audio pill/Play control appeared | **PASS** for attachment, audible playback not established |
| Save & cold reopen .xnote | Saved to library; save logs reported a 73,610-byte successful archive. Force-stopped only the app from library, cold-launched, reopened exact QA note. Ink, image and audio pill still visibly present | **PASS** for surviving ink/image/audio after process restart |
| .xnote archive inspection | Saved copy to `/sdcard/Download/QA_NT054_NATIVE_COPY_20261117.xnote`: 73,610 bytes; SHA-256 `6f441b89771fb96cc5761786ff6c5ab760086133a997d23768d7a1518369040c`; ZIP CRC clean. Members `assets/image-000.png`, `assets/audio-000.wav`, `flow.xml`, `manifest.json`. Embedded PNG and WAV hashes **exactly match their synthetic originals** | **PASS** for file integrity |
| .xnote text after reopening | Exact string `QA_NT054_TEXT_20261117` exists in `flow.xml` but was not visible in reopened editor screenshots on two opens. XML contains **49 paragraph nodes: 48 empty paragraphs preceding the typed text** | **FAIL for this visible recovery scenario**; cause unresolved, may involve text placement/flow pagination |
| Independent .xnote PDF export | From the same reopened QA note, `Export pages → Shareable PDF → All (1)` saved `/sdcard/Download/QA_NT054_NATIVE_TEXT_RENDER_20261117.pdf`: 16,158 bytes, SHA-256 `d3e7f79bef36418b34e7b71372f14b2873ab013130a48a93047c26a65d96ddaf`. Valid one-page A4 PDF; independent PyMuPDF text extraction returns **0 text characters** and no match for the typed QA marker; green ink still rendered (2,163 green pixels) | **CONFIRMS rendered export omits that saved text**, not just the editor screenshot |
| Infinite Canvas create/ink/image | Created `QA_NT054_MIXED_CANVAS_20261117`; green stroke appeared (2,997 green pixels). Imported exact synthetic PNG through file picker | **PASS**, smoke |
| Infinite Canvas audio | Scrolled the real toolbar to Insert and selected `Audio from file`, then only the exact synthetic WAV. Resumed from a temporarily interrupted tool response: app showed selected audio object with Play, Delete, Arrange etc., plus image and ink | **PASS** for attachment; **NOT VERIFIED** for audible playback |
| Save & cold reopen .xcanvas | Returned to library, saved native copy, cold-stopped/relaunched only the app from library; reopened exact QA canvas. Stroke, image and audio pill remained visible (post-reopen ROI 4,624 green / 15,232 blue pixels) | **PASS**, smoke |
| .xcanvas archive inspection | Saved copy to `/sdcard/Download/QA_NT054_NATIVE_COPY_20261117.xcanvas`: 73,262 bytes; SHA-256 `e20603dcfe90742523f32a60efb74be324b5fc4200ecf1c8143cf7a4ee78b606`. ZIP CRC clean. `manifest.json` has distinct `stroke`, `image`, `audio` items, plus the same two unmodified embedded media assets | **PASS** for file integrity |
| Native canvas editable text | B99 Infinite Canvas's on-screen All Tools and toolbar do **not expose a Text or Text Box action** in the checked UI. No native editable text item was added or verified | **NOT TESTED / BLOCKED by available UI**, not a proven source-level capability absence |
| Isolate text placement near top | Created a third QA-only note `QA_NT054_TEXT_TOP_20261117`. After ADB became authorized again, selected Text, tapped near upper page (`180,480` and subsequently `240,650`) and issued `adb input text`. The text was not visible, and the independently inspected native copy `QA_NT054_TOP_CONTROL_20261117.xnote` (741 bytes, SHA-256 `a8fc2d1910ed81b6fabca63aa2b1ab6508cdb3a35f37ffe58fe146d2cee005fb`) has a valid ZIP but empty flow (0 paragraph nodes, neither typed marker present). Text input did not reach storage in this control; the formatting toolbar appeared, but Android IME reported `mInputShown=false` | **INCONCLUSIVE INPUT CONTROL**; cannot prove or disprove issue #30. Repeat with a confirmed active caret/IME before exporting. |

## Assessment and handoff

**Do not mark NT-054 fully verified.** The native-media and cold-restart subset passed, but a saved text item failed to appear in the editor and the PDF renderer for the lower-page placement scenario. The 48 preceding blank paragraphs suggest a possible text-anchor/pagination issue, not proven data loss or an established root cause. The app's `FlowLayout` / `republishFlow` / exporter must be inspected and this scenario reproduced after USB debugging authorization is restored. Add a regression where placing text near the lower portion of a single-page note does not silently create invisible text or drop it from PDF export.

Before marking NT-054 complete, also verify an independent near-top text control, canvas-native editable text workflow if supported, interactive editability after reopen (not just visual presence), real audio playback and cancellation, asset recovery under interrupted writes, and background persistence. Broader corruption/atomic save fault injection belongs to NT-063.

**Device and final state:** ADB serial `RZ8W103G6EL` subsequently returned to `device` state. Further top-of-page control inputs were inconclusive (no text persisted), and a tap of the synthetic WAV play icon did not yield independent evidence of audible playback; it remains **NOT VERIFIED**. The app was safely returned to **My library** with its process running. No personal notes or images were used; no phone microphone, signed release, Gradle build or protected-branch changes occurred.
