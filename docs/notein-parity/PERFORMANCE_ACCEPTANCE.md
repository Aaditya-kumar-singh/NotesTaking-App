# NotesTaking App: smoothness verification plan

Reference: saved Notein APK analysis and NotesTaking native source, not a measured Notein tablet benchmark. The following are goals, **not passing results**.

## Measurements required

- Android Perfetto/System Trace or FrameTimeline on a physical Android tablet and phone; report p50/p95/p99 frame time, jank frames, memory and GC events.
- Record stylus pen-down to first provisional visible ink and pen-up to committed stroke; preserve pressure, tool type, input timestamps, history and pointer IDs.
- Compare before/after using the same device, refresh rate and disposable PDF/note fixtures. Target frame budget: 16.7ms at 60Hz; adapt to 90/120Hz hardware. Do not promise a frame rate without evidence.
- Test large vector and scanned PDFs, 200 pages, dense handwriting and erasing, orientation/split pane, 25/100/200/400% zoom and document save/reopen.

## Test matrix

| Scenario | Source gate | Device gate |
|---|---|---|
| Stylus + palm | Pointer ownership and cancel tests | No scroll/ink loss under mixed contacts |
| Finger ink/pan | Reversible user-facing input setting | Finger writes when enabled, pans when disabled |
| Read/Annotate | Stable PDF instance/viewport | 20 mode changes with no page reset |
| Partial eraser | Correct geometry/undo and bounded repaint | No full-page flicker during rapid erase |
| Pressure pen | Stroke/config roundtrip | Width/opacity faithful after save/reopen |
| PDF transforms | Rotation/zoom/crop coordinate tests | Ink anchors remain correct on all pages |
| Toolbar animation | No canvas re-create; accessible touch targets | Responsive phone/tablet, Back, font scale |
| Recovery/export | ZIP/SAF safety and output-file tests | No false success after cancellation/error |
| Native large-asset save | Non-closing ZIP wrapper forwards byte-array writes in bulk; verify exact bytes | Compare identical large note/canvas fixtures before and after on the same device; record elapsed save time, memory and reopened media integrity |

## Result record

Record NP and NT IDs, developer SHA, GitHub CI URL/APK hash, Android version and device, trace, fixture hashes, frame-time stats, first-ink latency, visual findings and remaining risk in PROGRESS.md.

No physical device is currently connected, so no smoothness benchmark is claimed.
