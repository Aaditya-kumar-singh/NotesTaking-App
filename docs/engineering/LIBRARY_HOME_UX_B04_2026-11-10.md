# NotesTaking App — Home / Library UX batch B04

Date: 2026-11-10
Branch: `feature/library-home-ux-b04`
Parent: B03 local commit `02d2a1e` (depends on B02 `1ba28f8` and B01 `9811900`)
Status: **Source implemented locally.** Not pushed or Android-tested; no Kotlin compilation, device screenshots or APK evidence.

## Experience changes

1. **One-tap primary action.** The library's floating action button stays labelled **New Note** and opens the existing note-creation flow immediately. When files are selected, the same button becomes **Export PDF** and retains the original export callback. New Canvas, New Folder, Import PDF and other actions remain in the existing header overflow menu. There is still only one primary FAB.
2. **Library identity and orientation.** At the top folder, the header presents **My library** as the screen title and the actual selected root-folder name below it. While browsing deeper folders, the existing clickable breadcrumbs replace the title without losing navigation. Recent and color-filter views retain their own header.
3. **One-tap Recent navigation.** A contextual chip in the scrolling filter row switches between Recent and Top folder at the root of the library. It is omitted during active text search, color filtering, and while inside a nested folder. Existing sidebar Recent navigation, filters, and sorting remain available.
4. **First-run storage choice.** The empty library prompts users to keep notes on this device or choose another folder, without requiring an account. **Use app storage** is the primary action; **Choose folder** remains explicitly accessible. The storage implementation and permissions are unchanged.
5. **Touch target sizes.** Explorer filter chips have a minimum 48x48dp clickable region, and the view-layout switcher, shared header actions, search pill and sidebar-opening control also use 48dp hit areas. The filter row has 56dp for spacing, and the selected-items toolbar grows to 56dp to fit its 48dp action buttons without clipping. Breadcrumbs and the embedded search-clear button need separate touch-target verification.
6. **Theme and translation safety.** New strings are in English and Simplified Chinese. Surfaces continue to use the existing Material/palette roles, shapes, search and selection states; no raw colors or replacement themes were introduced.

## Files

- `app/src/main/java/com/notestaking/app/ui/Backstage.kt`: Home header, contextual Recent chip, direct Create action, first-run choices, search sizing and filter-row spacing.
- `app/src/main/java/com/notestaking/app/ui/ExplorerChrome.kt`: Accessible hit targets for chips and header controls.
- `app/src/main/res/values/strings.xml` and `values-b+zh+Hans/strings.xml`: Localized Home title and first-run text.

No changes to existing document schemas, note data, thumbnail loading, saved explorer layout, sort/group choices, folder paths, permissions implementation, PDF output, ink, or undo history. The pre-existing untracked `.kotlin/` folder remains untouched.

## Acceptance checklist — pending GitHub CI/device QA

- [ ] GitHub Kotlin compilation, JUnit tests, lint and APK verifier pass on the exact published SHA.
- [ ] On a 320/360dp Android phone, the top controls and contextual Recents chip remain visible/scrollable with no clipping in search or when the IME opens.
- [ ] On a tablet (portrait/landscape) and in multi-window, the header adapts without overlapping folder breadcrumbs, sort controls or view switcher.
- [ ] New Note FAB opens note creation with **one** tap, not an intermediate menu. The top overflow still offers canvas, folder and PDF import.
- [ ] Selecting one or several files changes the FAB to Export PDF; the same export callback still runs and selection clearing remains correct.
- [ ] Recent chip opens Recent at the root; the Top folder chip restores folder browsing; folder, search, and color-filter views retain established behavior.
- [ ] First-run **Use app storage** initializes local storage and **Choose folder** still opens the platform picker; neither deletes or overwrites existing notes.
- [ ] Search/filter/switcher/icon buttons meet 48dp touch targets with readable TalkBack labels and sufficient contrast.
- [ ] Large font, Chinese localization, Light/Dark/OLED, keyboard, stylus/mouse navigation, selection, pinning and file operations are verified on hardware.
- [ ] Screenshot comparison against the previous Home layout is reviewed before merging or shipping.

## Deferred

- Full new visual hierarchy of note cards and folder thumbnails across every Explorer layout (Grid/List/Gallery/Timeline/Columns).
- New Note screen and PDF workspace redesign, expanded sort/filter persistence and optional first-run onboarding.
- Page/ink/eraser/Save As changes and other feature backlog tasks.

The existing Home and explorer had substantial code-backed enhancements before B04; those were retained, not reimplemented.
