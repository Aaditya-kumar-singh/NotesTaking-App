# NotesTaking App — Settings ZIP import hardening (B12 / Notein G03)

Date: 2026-11-11
Local branch: `fix/settings-backup-zip-budget-b12`
Parent: local B11 documentation/export change `a50bdf2`.
Reference: `docs/notein-parity/` NP-087 (settings backup/restore) and NP-083 (safe native ZIP import). The Notein APK analysis motivated the audit; **no Notein source or assets were copied**.

## Confirmed source issue

`Editor.readSettingsBackup()` previously used `ZipInputStream` and applied a per-entry/64 MiB aggregate cap **only to recognized members** (`settings.json`, `code-theme.toml`, and `fonts/*`). Unknown member contents and directory members were passed over without counting their expanded size against the settings backup's limit. Duplicate ZIP member names and unsafe path segments were not independently rejected in this entry point. A crafted archive could consume significant inflation/processing before the app reported an invalid settings backup.

## Smallest safe change

- Use the existing `BoundedZipInputStream` for this entry point (same checked decoder used for native notes): duplicate member names, absolute/traversal/control-char paths, and excessive per-member inflation fail closed.
- Keep the existing limit of **64 total members** and **64 MiB total expanded content**, plus the original limits for known JSON/font/theme data.
- For unrecognized members, retain forward-compatibility **by ignoring their meaning**, but consume and count their actual inflated bytes with the new `BoundedBytes.discard` streaming helper, limited by both the remaining total budget and the existing settings per-entry cap. This requires only a fixed 64 KiB buffer and avoids allocating unknown member payloads.
- All invalid archives return `null` through the existing `runCatching` boundary. The UI keeps its existing error message/confirmation logic, with no settings changes until a valid backup is explicitly accepted.

## Tests and manual verification

- New `BoundedBytesTest.discardedUnknownArchiveMembersCountTowardTheBudget` tests exact-limit, empty/zero-budget, overflow, and large repeated streams without full allocation.
- Existing `BoundedZipInputStreamTest` tests traversal and skipped inflated-entry rejection; settings backup now reuses the same parser. The test suite has **not been executed locally** under `AGENTS.md`.
- [ ] GitHub Android CI exact B12 SHA: compile, all tests, lint and APK.
- [ ] Export a settings ZIP, re-import, verify colors/pens/theme and no lost notes (NP-087).
- [ ] Feed disposable ZIP with one unknown 32+ MiB member; reject without heavy memory usage.
- [ ] Feed duplicate `settings.json` and `fonts/../invalid.ttf` and check controlled failure.
- [ ] Provider cancellation, truncated ZIP, low storage, and app relaunch do not corrupt existing settings.
- [ ] A settings backup containing a small unknown future-version member remains importable.

No file-format extension or database mutation; no local Gradle/Android build or device installation. B08 is CI-passed; B09 published CI, B10/B11/B12 not published. Existing `.kotlin/` remains untouched.
