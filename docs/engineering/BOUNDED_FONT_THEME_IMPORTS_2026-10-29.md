# Bounded custom font and code theme imports — 2026-10-29

**Branch:** `fix/bounded-font-theme-imports`, based on `main` at `5a93bd7`. Independent of still-running stylus input branches.

## Verified issue

The system SAF picker callbacks for Helix code themes (.toml) and custom fonts (.ttf/.otf) previously loaded the entire untrusted provider stream using `readBytes()` on the UI thread. A malicious or unusually large document could exhaust the JVM heap or freeze the tablet while the app waits for the document provider. Theme and font importer methods also lacked an explicit size cap, meaning imported settings backups could bypass any picker-only limit.

## Implemented

- `BoundedBytes`: standard maximum **1 MiB per code theme** and **16 MiB per font**; streams count actual bytes even when provider metadata is missing or deceptive. The bounded reader now gives up on a provider returning zero bytes 17 times without progress, avoiding an infinite CPU loop.
- `MainActivity`: theme and font SAF reads run on `Dispatchers.IO`, return to the UI coroutine for notifications/application, and report read failures using existing localized error messages.
- `Editor.importCodeTheme` and `FontCatalog.importFont`: enforce caps before invoking a parser or writing imported bytes to app storage. Font paths also used by backup application respect the same limit.
- `Editor.readSettingsBackup`: cap per-theme/per-font ZIP entries, maximum **64 entries**, and **64 MiB combined recognized payload**. Oversized settings backups are rejected before changing settings.
- `BoundedBytesTest`: verify exact sizes, oversized theme streams, streamed font-sized payloads, providers stalled with no bytes, and intermittent zero-byte reads.

## Scope and tradeoffs

- Legitimate font files over 16 MiB and code themes over 1 MiB are deliberately rejected. The previous settings/theme remain unchanged when parsing or reading fails.
- Some explorer metadata files and template-image inputs elsewhere in the app still use unbounded reads/copies and should be audited separately; this phase does not claim every SAF entry point is hardened.
- Code-theme parse and font registration still run on the main thread *after* bounded file reading. These are substantially smaller inputs now, but heavy font parsing may still affect responsiveness and needs device profiling.
- GitHub Android CI is required for Kotlin unit tests, lint and APK assembly. There is no local Android SDK / Java compile in this workflow, and no physical tablet tests yet.
- The unrelated untracked `SKILLS/` directory is left untouched. No release APK or user notes are modified.
