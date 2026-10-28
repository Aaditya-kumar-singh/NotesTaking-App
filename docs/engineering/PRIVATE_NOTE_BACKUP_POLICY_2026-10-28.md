# NotesTaking App — Private-note backup policy (2026-10-28)

**Branch:** `fix/private-note-backup-policy`
**Baseline:** `main` at `ad7d90a` (the import hardening is separately under CI).
**Release status:** Implemented but **not yet Android-CI or device verified**.

## Decision: privacy first, no silent automatic copy of notes

The current app handles handwritten notes, annotated PDFs, audio recordings, and recoverable editing sessions. Prior to this change its manifest used `android:allowBackup="true"` with no rules. Android's default backup mechanism may include internal app files, and the behavior of device-to-device (D2D) transfers can differ from cloud backup. Since the app currently does not provide an in-app encrypted backup opt-in or key management, automatic extraction of a user's private working documents should be disabled rather than silently transferring their contents.

**Important product tradeoff:** This also disables automatic migration/restoration of the user's own notes and preferences to a new Android device. Users must export/save their notes to a location they control (via the app's SAF/document export and sharing workflows). A future optional, user-controlled encrypted backup/import feature should be planned and tested before changing this policy. The code does NOT delete existing saved notes or cloud backup copies already held by an Android backup provider.

## Implementation

1. `AndroidManifest.xml` — `android:allowBackup="false"`, `android:fullBackupContent="@xml/backup_exclusions"` (legacy Android) and `android:dataExtractionRules="@xml/data_extraction_rules"` (Android 12+).
2. `res/xml/backup_exclusions.xml` — excludes all app-managed private data domains for Android 11 and earlier.
3. `res/xml/data_extraction_rules.xml` — explicitly excludes both `cloud-backup` and `device-transfer` domains for newer Android. This is intentional because `allowBackup="false"` may not by itself suppress device-to-device copying on every device manufacturer.
4. `tools/ci/check_backup_privacy.py` — checks the manifest attributes, no inadvertent XML `include` rules, and complete exclusions for all listed storage domains. It runs on every GitHub Android CI source-check job.

Both app-private `filesDir` (sessions, working documents, recordings, fonts, config, templates) and app-specific `getExternalFilesDir` (Notes directory served by DocumentsProvider) are covered. The user can still deliberately share/export a note through system SAF or share sheets.

## Verification

- Run `python tools/ci/check_backup_privacy.py` and `python tools/ci/check_project.py`.
- GitHub CI must compile the manifest/XML on Android SDK 36, run tests/lint and produce an APK. Verify the **merged manifest** and included compiled backup-rule resources.
- On an emulator with `adb shell bmgr`, inspect backup transport behavior. On Android 11 and Android 12+, ensure private files are excluded from backups and from supported device-to-device transfers.
- Confirm manual Export/Share/SAF Save As still works; no data should be erased or modified by backup policy changes.
- New Android versions/OEM transfer apps could differ; this is a policy and defense-in-depth configuration, not a guarantee that root-level devices or third-party tools can never read files.

## Official references

- https://developer.android.com/identity/data/autobackup
- https://developer.android.com/about/versions/12/behavior-changes-12
- https://developer.android.com/privacy-and-security/risks/backup-best-practices

## Open follow-up

Research optional *user-controlled, end-to-end-encrypted note backup and import*, migration UX, clear restore instructions, storage quotas, device lock requirements and accessibility. Do not automatically enable cloud backup again until privacy and data-recovery choices are explicit.
