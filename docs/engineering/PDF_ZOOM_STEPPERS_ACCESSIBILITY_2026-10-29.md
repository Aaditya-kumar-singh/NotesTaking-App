# PDF filter and zoom stepper accessibility — local source checkpoint

**Branch:** `feature/settings-toolbar-a11y` (unpublished)
**Status:** Source implementation only; GitHub compilation, UI rendering and device testing still required.

## Change

- PDF contrast, invert, brightness and sepia steppers now use `IconButton` with 48x48dp hit targets, localized *Decrease/Increase [setting]* labels, and disabled states at their supported minimum/maximum.
- Minimum and maximum zoom-limit steppers use the same labeled, 48dp buttons and disable when their limit is switched off or at its global boundary. Existing `onValue` callbacks and cross-limit clamping remain unchanged.
- The 280dp zoom popup presents each label/switch above its 48dp steppers and up-to-1600% value. This prevents the former single row from overflowing a narrow popup.
- The min/max zoom switches announce the associated setting label.
- Icons use the existing NotesTakingIcons minus/plus glyphs. New accessibility descriptions are provided for English and Simplified Chinese.

## Verification and device acceptance

- Local source/resource assertions, XML parsing, privacy checker and `git diff --check` must pass.
- GitHub Android CI must compile the exact published batch SHA, run Kotlin tests and lint, produce and verify the APK. **Do not claim a local Kotlin build was performed.**
- Android TalkBack: confirm the four PDF filter labels and two zoom-limit labels read correctly, enabled/disabled states are announced and +/- gestures adjust settings once per activation.
- Check PDF filter values at 0/100/200 where supported; zoom limits at 20 and 1600%, and min/max cross-boundary behavior with one or both switches disabled.
- Test the 280dp popup on compact phones and Android tablets at normal and 200% font scaling, portrait/landscape, and with keyboard navigation.
- Verify existing note/PDF settings persist through reopening the popup; no storage, renderer, document model or drawable data was edited.
