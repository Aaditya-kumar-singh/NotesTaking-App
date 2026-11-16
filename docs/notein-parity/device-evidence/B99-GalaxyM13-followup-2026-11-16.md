# B99 Galaxy M13 — follow-up real-device QA (2026-11-16)

**Status:** Observed smoke-test evidence for the already installed B99 universal debug APK; **not full NT acceptance or REL-16 signoff**. No production keys, personal notes, or private Downloads files used.

## Exact test environment

- Device: Samsung Galaxy M13 (`SM-M135F`), Android 14 / API 34, 1080×2408px, `armeabi-v7a`.
- APK: `com.notestaking.app.debug`, version `0.1.0-debug`, verified installed SHA-256 `25eaf748dbe1c2039c66aa383f7ecc42bd9de2aeb3043dbc47f24fcb3b248025`, Git commit `b299cbb8972267eebe103493fa093f7e5cfa6f70`.
- GitHub: [B99 all-ABI CI #43857426156](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/43857426156), artifact `NotesTakingApp-debug-all-abis-200`.
- Earlier note-creation, two-stroke save, area-eraser Undo/Redo evidence: [B99-GalaxyM13-smoke-2026-11-16.md](B99-GalaxyM13-smoke-2026-11-16.md).
- **Only disposable test data:** `QA_GalaxyM13_20261116`, `QA_Canvas_M13_20261116`, `QA_Imported_B99_20261116`, and local Downloads export `QA_NotesTaking_B99_DeviceTest_20261116.pdf`.

## Observations from the second real-phone test round

| Check | Result | Reproducible evidence |
|---|---|---|
| New Infinite Canvas creation | **PASS (phone smoke)** | Library More → New Canvas → unique QA name → Create; canvas appears with `Canvas` kind, and opens to a writable grid. Note: creation returned to the library instead of automatically opening; not yet classified as a defect. |
| Infinite Canvas finger ink | **PASS** | ADB swipe on the empty disposable grid changed green-ink count from 0 to 2,403, with 2,572 changed screenshot pixels in the page ROI. |
| Canvas Undo/Redo | **PASS (synthetic keyboard)** | Ctrl+Z reduced detected green ink to 0; Ctrl+Shift+Z restored 2,403. Physical keyboard and real stylus not tested. |
| Canvas save/Home/reopen | **PASS** | Green pixels 2,403 before Home, 2,403 after reopening, with successful `notestaking.save` logs. |
| Canvas cold process restart | **PASS (smoke)** | After saving, `am force-stop`, `am start -W` reported `Status: ok`, `LaunchState: COLD`, one `TotalTime: 5702ms`. QA canvas reopened with the same 2,403 green pixels. One launch is not a benchmark. |
| Library positive search | **PASS** | Search `QA_Canvas`: one matching result, canvas shown and original QA note excluded; search-clear restored two files. |
| Library negative search | **PASS** | Query `QA_NotFound_987654`: `No notes match` empty state; no QA note shown. Clearing restored two files. |
| Library view modes | **PASS (smoke)** | Switched from Grid → List (both QA file titles present), then explicitly restored original Grid. Other view preferences were not changed. |
| PDF export picker cancellation | **PASS (smoke)** | Original QA note menu → Export to PDF → Android Downloads SAF Save picker; Back returned to the app, which stayed running. No success toast/partial file was claimed from the cancelled run. |
| PDF export and independent parse | **PASS** | Same disposable two-page note exported as `QA_NotesTaking_B99_DeviceTest_20261116.pdf` to Downloads; 13,770 bytes, valid `%PDF-1.4` header and `%%EOF`, valid PyMuPDF parsing, exactly **2 A4 pages** (~595.3×841.9 pt), rendered green-ink pixel counts **1,290** (page 1) and **656** (page 2). |
| Exported PDF hash | **RECORDED** | SHA-256 `633905d39cd8bf82267430b865ddb63d8851ac3c8653fac9e680f6c46faca083`. This is a QA-only file, retained in phone Downloads. |
| PDF import | **PASS (phone smoke)** | Library More → Import PDF → select **exact QA PDF only** from Android Downloads → unique QA import name → Save. After scrolling the library, imported note was present; three QA library files were confirmed. |
| Imported PDF rendering | **PASS (smoke)** | Opened `QA_Imported_B99_20261116`, visually confirmed the original strokes and both page surfaces. |
| PDF Read/Annotate toggle | **PASS (smoke)** | `Annotating PDF - tap to read` changed to `Reading PDF - tap to annotate`; switching back restored Annotate. |
| PDF annotation persistence | **PASS** | Added a new green annotation on first imported PDF page, upper-region ink 0→1,705; Home/reopen gave 1,706 pixels (minimal anti-alias difference), with successful note save/read logs. |
| Original PDF immutability | **PASS** | Re-read exported source PDF after import and after annotation. Exact SHA-256 unchanged (`633905...6c46faca083`). |
| Side-panel page inventory | **PASS** | Pages side panel listed `Page 1` and `Page 2` thumbnails, header `Page 2 of 2`. |
| Side-panel Page 1 tap navigation | **INCONCLUSIVE** | Tapping Page 1 thumbnail then Page 2 left `Page 2 of 2` header unchanged, and right-viewport screenshot difference was zero. Both PDF pages fit fully in the narrow preview pane; this does **not** prove a broken navigation action. Needs larger viewport/zoom and page-anchor test before logging a defect. |
| App stability and last state | **PASS for tested paths** | App process stayed running, checked Android crash buffer contained no `Process: com.notestaking.app.debug` crash. Returned to Home library displaying `3 files`. All QA files retained for further tests. |

## Acceptance interpretation / remaining work

- These phone-smoke observations partially exercise NT-012, NT-014, NT-026, NT-054, NT-055, NT-060 and document storage/recovery. **Do not mark these full cases as PASS:** several require many more notes, `.xcanvas` assets, page selection/ranges/PNG, interrupted provider reads, tablets, third-party PDF viewers and exact failure injections.
- Not yet covered: PDF crop/90°–270° rotation and 25–400% zoom, pin alignment under transforms, split pane keyboard focus, TalkBack and 2× font, 200-page PDF memory/Perfetto, offline/denied permission, image/audio/text asset round-trip, handwriting recognition, S Pen pressure/palm/tilt, and signed production release.
- The independently exported PDF was checked with PyMuPDF for page structure and rendered green-ink presence. The import note was saved separately; **the source PDF was never overwritten**.
- No app uninstall, Android permission revocation, network toggle, personal file import, user-account modification or production note deletion was performed. An unrelated downloads listing was displayed by the system picker but no unrelated file was opened.
- This is evidence suitable for coordinator review; **REL-08–REL-13 and REL-14/16/19/20 remain open** until their full acceptance and signing gates are satisfied.
