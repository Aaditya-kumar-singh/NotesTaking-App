# NotesTaking App

Android note-taking, handwriting, stylus, drawing, PDF reading and annotation app.
Built with Kotlin, Jetpack Compose, bundled Tree-sitter and PDFium.

## Build requirements

- JDK 17
- Android SDK (compile SDK 36)
- Android NDK 27.0.12077973
- CMake 3.22.1

Run `gradlew.bat assembleDebug` on Windows, or `./gradlew assembleDebug` on macOS/Linux.

## Compatibility

NotesTaking App supports existing `.xnote`, `.xcanvas` and `.xtemplate` file formats.
Changing the Android application ID means it installs separately from the original XNotes app.
Export existing user notes before uninstalling the old application.

## Open-source attribution

NotesTaking App is a modified version of the MIT-licensed XNotes project by Shardul Vikram Singh.
Original MIT copyright and permission notice are preserved in [LICENSE](LICENSE).
Third-party dependencies retain their applicable notices and license files.
See [NOTICE.md](NOTICE.md) for details.

## GitHub development and automation

All GitHub Actions automation is stored in [`.github/workflows/`](.github/workflows/). The workflows validate the project, build debug APKs, run tests and lint, scan Java/Kotlin code, monitor dependencies, and support manually approved signed releases. See the [GitHub workflow setup guide](docs/github/WORKFLOWS.md) for repository settings, required release secrets, and the full CI/CD process.

The workflow files are local until this project is uploaded to a new GitHub repository. They do not initialize Git or publish anything by themselves.

## Project state

The app is under local rebranding and validation. No official Google Play or F-Droid listing is claimed.
Some previously recorded correctness and security findings still require fixes and Android device testing.
