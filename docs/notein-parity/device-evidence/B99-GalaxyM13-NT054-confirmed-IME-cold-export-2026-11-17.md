# B99 Galaxy M13 - NT-054 confirmed-input top-page recovery control

**Date:** 2026-11-17 (Asia/Kolkata). **Verdict:** **FAIL for text visibility and PDF rendering; NT-054 NOT VERIFIED.** Additional evidence for [GitHub issue #30](https://github.com/Aaditya-kumar-singh/NotesTaking-App/issues/30). See the original [mixed-content report](B99-GalaxyM13-NT054-native-mixed-persistence-2026-11-17.md) for earlier lower-page testing.

## Baseline and safeguards

- Device: physical Galaxy M13 (SM-M135F), Android 14, ADB serial `RZ8W103G6EL`, authorized.
- Existing installed B99 debug package `com.notestaking.app.debug`; source `b299cbb8972267eebe103493fa093f7e5cfa6f70`, base APK SHA-256 `25eaf748dbe1c2039c66aa383f7ecc42bd9de2aeb3043dbc47f24fcb3b248025`. No reinstall, build or release action.
- Reused only disposable note `QA_NT054_TEXT_TOP_20261117`, whose earlier attempt had failed to deliver input to the IME/storage. Did not edit the original lower-page failure fixture or personal notes.

## Repeat with confirmed text focus

1. Opened the disposable note from My library. The Pages side panel was visible; used Android's `input keycombination 113 30` (Ctrl+B, the editor shortcut) to close it, ensuring page taps targeted the canvas rather than the panel.
2. Selected **Text** via **All tools**, tapped the actual page near `(530,445)`. Android input-method dump reported `mInputShown=true` and `mIsInputViewShown=true`, unlike the previous inconclusive test (`mInputShown=false`).
3. Sent the distinct synthetic marker `QA_NT054_IME_CONFIRMED_20261117`; returned to My library to save.
4. Used **Save a copy...** to save `/sdcard/Download/QA_NT054_IME_TOP_CONFIRMED_20261117.xnote` (789 bytes; SHA-256 `388d706c9ce47b6094d4ef65d3a7b90e264aec3636adbbf1deda10c2d40c5005`). Independently read its valid ZIP: `flow.xml` has three paragraph nodes, **two empty** followed by the exact marker. **PASS: input reached persisted native data.**
5. Force-stopped only NotesTaking from its library state; cold-restarted and reopened the same note. A real device screenshot showed a **visibly blank page**, without the stored marker. **FAIL: text not visible after reopen.** This is independent of the previous 48-empty-paragraph lower-page fixture.
6. From that cold-reopened note saved `Export pages > Shareable PDF > All (1)` as `/sdcard/Download/QA_NT054_IME_COLD_RENDER_20261117.pdf` (1,757 bytes; SHA-256 `b5f03a516d3575a3781f49b6ba01587dba66399e3e4790a257eaee6aa5836414`). Independent PyMuPDF checks: valid one-page A4 (595.28 x 841.89 pt), **zero extractable text characters**, **zero embedded images**, and **zero non-white pixels** when rasterized at default resolution with RGB channels thresholded below 245. **FAIL: exported page is blank.**
7. Returned to library and saved another native copy `/sdcard/Download/QA_NT054_IME_AFTER_COLD_20261117.xnote` (789 bytes), independently inspected it, and confirmed the marker and two empty preceding paragraphs **still persisted after cold restart**. Its SHA-256 equals the pre-restart copy **byte for byte**: `388d706c9ce47b6094d4ef65d3a7b90e264aec3636adbbf1deda10c2d40c5005`.

## Assessment

This control rules out the earlier failed-input explanation for the new case. A properly focused Text tool accepts input and persists it in native storage, but the cold-reopened page and PDF renderer both omit the stored content. Two empty preceding paragraphs do not, by themselves, establish a root cause or prove all text positions fail. Investigate flow pagination/cursor anchoring, loaded text visibility and PDF flow export as a **P0** issue in the authorized A/coordinator source lane; add regressions for top- and lower-page text, restart and exported PDF. **No source patch has been attempted in this QA run.**

Known original fixture integrity remains unchanged: mixed `.xnote` SHA-256 `6f441b89771fb96cc5761786ff6c5ab760086133a997d23768d7a1518369040c`; original synthetic two-page QA PDF SHA-256 `633905d39cd8bf82267430b865ddb63d8851ac3c8653fac9e680f6c46faca083`. The phone ended at My library, NotesTaking process running. Editable canvas text, independent audio playback, low-storage and provider interruption requirements still pending. **No release or triad device-verification sign-off.**
