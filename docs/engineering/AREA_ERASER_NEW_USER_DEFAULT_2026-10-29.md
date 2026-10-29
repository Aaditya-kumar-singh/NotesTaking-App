# Area Eraser default for new users — 2026-10-29

**Branch:** `feature/area-eraser-default-new-users` (from `main`, independent of the pending integration branch).

## Behavior
- Newly initialized eraser settings select **Area** mode, so first-use erasing trims touched ink rather than deleting entire strokes.
- Existing saved settings containing `"erase_mode": "stroke"` stay in Stroke mode. Users who deliberately selected Stroke can continue to choose it via the existing eraser settings; no migration/rewrite of their preferences occurs.
- The persisted eraser size remains **24 content pixels as a radius**; no reinterpretation of the setting and no display-size migration.
- The low-level `ToolConfig()` default remains Stroke for backwards compatibility; only the `ToolDefaults.configFor(Tool.ERASER)` factory changes. Unknown explicit saved mode IDs also retain their legacy fallback.

## Tests
`ToolsTest` verifies that the new eraser factory configuration selects Area while retaining existing width. `SettingsTest` verifies that fresh settings select Area, a saved Stroke preference loads and round-trips unchanged, and a subsequent explicit Area choice persists.

## Acceptance and limitations
GitHub Android CI must run unit tests, lint and debug APK assembly before merging. Device testing still must check actual stylus and finger erasing, hover cursors, the eraser mode picker and accessibility on a low-memory tablet. The physical precision eraser implementation is tracked in `PRECISION_AREA_ERASER_CLIPPING_2026-10-29.md` and is still separately gated by the integrated build. This change does not override existing users' persisted mode.
