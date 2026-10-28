# Contributing to NotesTaking App

Thanks for improving the Android app. The repository is maintained using GitHub issues, pull requests and Actions.

## Development

Build with JDK 17, Android SDK 36, NDK 27.0.12077973 and CMake 3.22.1. The Gradle wrapper pins Gradle 8.13.

Before opening a PR:

1. Run `python tools/ci/check_project.py`.
2. Run `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`.
3. Check your changes on an emulator or device when Android UI, PDF, stylus or storage is affected.
4. Preserve existing `.xnote`, `.xcanvas`, `.xtemplate` and legacy-backup compatibility.
5. Never commit keys, user documents, signing files, developer SDK paths or generated audit records.
6. Keep the original MIT license, attribution and third-party license notices intact.

Explain tests, screenshots, compatibility risks and rollback considerations in your PR. Open an issue before large changes to document formats, native/JNI bindings, security boundaries or storage.

See [GitHub workflow documentation](docs/github/WORKFLOWS.md) for CI, signed releases, security scans and dependency automation.
