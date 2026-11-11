# NotesTaking App — B09: stylus/eraser input near the page scrollbar

Date: 2026-11-11
Branch: `feature/stylus-eraser-scrollbar-b09`
Parent: published B08 `5633d01` (Android CI still running at B09 kickoff)
Status: Local source implementation; not pushed/merged/device-verified.

## Verified code-level issue

In the paged document/PDF canvas, `CanvasView.trackScrollbar()` inspected the right-side scrollbar touch band (or bottom band in paginated mode) before passing the touch to `InteractionController`, but did **not** check the pointer's tool type. A stylus tip or the pen's eraser end landing in that band was consumed as a scrollbar drag. This causes page motion rather than ink/erasing, particularly on a tablet when writing near page edges.

The same interceptor also swallowed all extra pointers when a *finger* scrollbar drag was active. A pen arriving as a second contact could never reach `PenContactRouter` for the already-supported finger-first/stylus-second takeover.

## Bounded correction

1. Add one pure classification helper to existing `PenContact`: `canStartScrollbar` returns false for stylus and pen-eraser tip. Finger/mouse behavior is unchanged.
2. Check that rule on scrollbar `ACTION_DOWN`, before touching scrollbar state or page position. Pen / eraser events instead reach the existing editor input pipeline.
3. During a finger scrollbar drag, an `ACTION_POINTER_DOWN` from a stylus or eraser releases only the scrollbar-drag state and forwards the event to the pre-existing `PenContactRouter`. That router synthesizes the new pen's own `ACTION_DOWN` and prevents a resting finger from becoming a competing pinch.
4. Regular extra fingers remain consumed by the scrollbar during its drag; terminal `UP`/`CANCEL` clean up as before. No change to the scrollbar visual, layout or wheel/gesture configuration.
5. Extend `PenContactTest` to cover both pen pointer types, finger/mouse scrollbar eligibility, and finger-first pen/eraser handoff.

No changes to `Stroke.erasedBy`, `EraserSweep`, `EraseMode`, `InteractionController`, `PenContactRouter`, document serialization, note save/reopen, rendering caches, undo, page positions, or user preferences. Existing partial/stroke eraser options and their stored values remain untouched.

## Source-only checks

- [x] Python project/resource checker.
- [x] Python backup/privacy checker.
- [x] `git diff --check`, and source-level callback/policy assertions.
- [x] Regression test written and reviewed; not executed locally under `AGENTS.md`.

## Cloud/device acceptance still required

- [ ] GitHub Android CI for this exact B09 SHA: Kotlin compilation, tests including `PenContactTest`, lint and APK verification.
- [ ] Tablet + stylus: draw a stroke beside the right scrollbar when vertical scrolling is enabled; no scrollbar movement. Draw/erase near the bottom scrollbar in paginated mode.
- [ ] Finger drag on both scrollbar orientations still changes page position and ends cleanly; mouse/trackpad scrollbar interaction remains usable.
- [ ] Finger holds or drags scrollbar then stylus writes: pen takes over without residual scroll, pinch, or phantom line. Repeat with the pen's eraser tip.
- [ ] Ordinary pen/finger two-pointer ink, palm rejection, zoom, pan, stylus hover/side-button and page UI remain unchanged.
- [ ] Area eraser only removes the touched stroke segment; Stroke eraser still removes whole stroke when selected. Test at varying zoom levels, undo/redo and save/reopen.
- [ ] Verify no flicker on area erasing in a heavily annotated, multi-page PDF and in infinite canvas; the rendering/caching pipeline was not changed here.
- [ ] Run on small phone and actual tablet, including landscape, split view and PDF zoom/fit. Check saved content and gesture cancellation.

**Note:** This fixes a confirmed interception path in source. It is not a claim that all stylus/palm issues or area-eraser flicker are resolved. Those require hardware reproduction and separate investigation.

Pre-existing untracked `.kotlin/` directory remains untouched.
