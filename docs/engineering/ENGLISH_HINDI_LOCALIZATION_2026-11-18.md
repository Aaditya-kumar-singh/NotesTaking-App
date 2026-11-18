# English and Hindi UI localization — 2026-11-18

## Scope

NotesTaking App offers **English (en-US, default)** and **Hindi (hi)**. Simplified Chinese is no longer an app UI locale. Existing notes, imported documents, filenames, PDF text extraction, Devanagari handwriting support, Chinese glyph rendering and original software attribution are not changed.

## Implementation

- Removed the obsolete `app/src/main/res/values-b+zh+Hans/strings.xml` resource.
- Added six Hindi resource XML files under `app/src/main/res/values-hi/`, covering **all 1,121 translatable string keys, 21 plural resources and 3 string arrays** from `values/strings.xml`. The brand's non-translatable `app_name` remains `NotesTaking App`.
- Left `app/src/main/res/resources.properties` at `unqualifiedResLocale=en-US`.
- In `app/build.gradle.kts`, retained automatic `generateLocaleConfig = true` for Android 13+ and added `localeFilters += listOf("en", "hi")` so dependency locales are not accidentally bundled.
- Replaced the previous Chinese-coverage rules in `tools/ci/check_project.py` with Hindi resource completeness, duplicate/unknown key detection, plural `other` forms, positional-format compatibility and array-length checks. Missing translations or a restored Chinese UI locale now fail source validation.
- Documented where to select the preferred language in `README.md`.

## QA acceptance

1. On Android 13+ (including the Galaxy M13), open **Settings > Apps > NotesTaking App > Language**: English and Hindi should be the available choices; Chinese must not appear. Older Android follows device locale.
2. Inspect Hindi in Home, new note, Canvas pen variants, eraser sizes/modes, Undo, Save As, PDF import/export, toolbar customization, Trash and version history. Test a normal phone and narrow split-pane at 1.3× and 2× font scale for truncation, clipped buttons and text overlap.
3. Check TalkBack announcements, keyboard focus, Devanagari joining/diacritics, filename input, string formatting (%1$d and %1$s), singular/plural counts and the month/day arrays.
4. Verify switching English → Hindi → English does not change note content, document format, saved toolbar settings, data privacy or storage URIs. Opening a PDF containing Chinese characters must still work; removing UI localization must never remove the underlying PDF text decoder.
5. Real Android compilation, resources linking, lint, Kotlin/JUnit tests, generated locale config and APK assembly must pass **on the exact QA SHA** in GitHub Actions. This project prohibits local Windows Gradle builds.

## Status

The local source checker, backup/privacy checker, backlog checker, rebranding verifier and whitespace checks have passed. **Android CI, Hindi linguistic review and real-device UI validation remain pending at authoring time.** A resource-key/printf match is not proof that a translation reads naturally or fits on every display. Do not close localization QA until these checks are recorded.
