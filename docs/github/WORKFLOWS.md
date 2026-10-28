# NotesTaking App — GitHub automation

This directory documents the GitHub setup for the Android codebase. The workflows were authored locally; **they have not been executed on GitHub yet**. The app has **no local `.git` directory or configured GitHub remote** until you decide to publish it.

## What's automated

| File | When | What it does |
| --- | --- | --- |
| `.github/workflows/android-ci.yml` | Push to `main`/`develop`, pull request, manual | Verify package/resource/license structure, install JDK 17 + Android SDK 36 + NDK + CMake, run Gradle unit tests/lint, create a debug APK, verify APK/native libraries, upload build artifacts and test reports. |
| `.github/workflows/android-release.yml` | Manual only | Check an existing Git tag against `versionName`, use GitHub Secrets to sign the release APK, verify signature, upload signed APK + SHA-256, optionally create a **draft** GitHub Release. |
| `.github/workflows/dependency-graph.yml` | Main branch Gradle changes, manual | Resolve and submit Gradle dependency graph to GitHub dependency monitoring. |
| `.github/workflows/codeql.yml` | Main Kotlin changes, weekly, manual | Build Kotlin source for CodeQL and upload Java/Kotlin security findings (requires repository code-scanning availability). |
| `.github/dependabot.yml` | Weekly | Open PRs for Gradle and GitHub Actions dependency updates. |
| `.github/ISSUE_TEMPLATE/` and `.github/pull_request_template.md` | GitHub UI | Standardize issue reports, feature requests, changes, testing, and compatibility checks. |

## One-time GitHub configuration

1. Create a **new** repository in your GitHub account, for example `NotesTaking-App`. Do not reuse the old XNotes upstream repository or its remote.
2. Upload the current NotesTaking App source, including the `.github/` folder, using the GitHub web interface or a newly initialized Git repository when you choose. The automation files alone cannot upload the source.
3. In your repo's **Settings → Actions → General**, enable workflows and set the default `GITHUB_TOKEN` to read-only where the UI offers that option. The release and dependency graph workflows request the specific write permission they need.
4. Set the default branch to `main`. The CI and scheduled workflows assume `main`, with `develop` also supported for CI.
5. Add branch protection/rulesets for `main`: require a pull request, require the **Source, resources and attribution** and **JDK 17 / tests / lint / debug APK** checks after their first successful run, block force-pushes, and optionally require reviews. Do not enable a check before its first run appears in the UI.
6. Enable GitHub Dependency graph, Dependabot alerts, Dependabot security updates and Code scanning if the repository and your account plan support them.
7. Review Actions runs to confirm that the chosen GitHub-hosted runner has enough disk/CPU for the large vendored PDFium/Tree-sitter builds. If it doesn't, move these workflows to a suitable larger runner rather than silently skipping build failures.

## Releases: configure signing without committing secrets

Create a GitHub environment called `release` in **Settings → Environments**, optionally requiring a reviewer. Add these as **repository or environment Actions secrets**:

| Secret | Contents |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Entire release/upload keystore, Base64-encoded (single line). |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore store password. |
| `ANDROID_KEY_ALIAS` | Signing key alias. |
| `ANDROID_KEY_PASSWORD` | Signing key password. |

Never commit the keystore, passwords, `keystore.properties` or `local.properties`. The release workflow reconstructs signing files only on the GitHub runner and deletes them when the job ends.

**Important:** Keep a safe offline backup of your signing key and passwords. Losing them may prevent you from updating a previously distributed app under the same signing identity.

### Create your first release

1. Confirm the app's `app/build.gradle.kts` has `versionName = "0.1.0"` and `versionCode = 1`.
2. After CI has passed and device/compatibility testing is satisfactory, create and push a tag named `v0.1.0` in the new repository.
3. Open **Actions → Signed Android Release → Run workflow**, provide `v0.1.0`, and leave **Create draft release** enabled.
4. GitHub will build and sign the APK, check its package and embedded JNI libraries, verify the signature, and attach the APK plus SHA-256 checksum to a **draft** GitHub Release. Review it before publishing.
5. For later releases, bump **both** `versionCode` (strictly increasing) and `versionName`, then create a matching new tag.

The workflow intentionally **does not auto-publish** a public release, does not automatically submit to Google Play, and does not manufacture APKs when build or test steps fail.

## Development workflow on GitHub

Make changes on a branch such as `feature/pdf-annotations`; open a pull request to `main`. GitHub Actions runs source checks, unit tests, lint and debug build. Review the resulting reports and debug APK attached to the Actions run. After approval and passing checks, merge. Dependabot opens dependency update PRs instead of silently modifying your codebase.

For a manually requested debug APK, open **Actions → Android CI → Run workflow**, then download the APK from the completed job's **Artifacts** section. Debug APKs use the separate application ID `com.notestaking.app.debug` and should not be used as production releases.

## Security and limitations

- This project is a modified MIT-licensed version of XNotes. The original `LICENSE`, `NOTICE.md`, PDFium, Tree-sitter and other third-party notices **must remain**.
- Keep the legacy `.xnote`, `.xcanvas` and `.xtemplate` formats readable.
- The existing 45 historical audit findings are **not resolved by adding CI**. They require source fixes, regression tests, malformed-input tests and device validation.
- CodeQL analyzes Java/Kotlin here; it does not yet claim complete coverage of vendored C/C++ PDFium, FreeType or Tree-sitter native code. Advanced C/C++ scanning would need suitable build instrumentation.
- Android emulator/stylus testing is not included in these initial workflows; physical stylus behavior, split-screen, device storage providers and crash recovery require device or emulator tests.
- The first GitHub build might fail because this heavily vendored native app has not yet completed a post-rebrand Gradle build. Fix actual errors; don't use `continue-on-error` to create false green checks.
- GitHub-hosted runners and security features are subject to your plan and repository's visibility. Some workflow runs or large native builds may incur usage costs.

## Local validations

From the project root:

```powershell
python tools/ci/check_project.py
python tools/ci/check_release_tag.py v0.1.0
python -m py_compile tools/ci/check_project.py tools/ci/verify_apk.py tools/ci/check_release_tag.py
```

On a machine with JDK 17, Android SDK/NDK and CMake installed:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

The scripts under `tools/ci/` are the same source and APK verification commands the GitHub Actions runner uses.

## References

- GitHub Actions documentation: https://docs.github.com/actions
- Gradle on GitHub Actions: https://docs.gradle.org/current/userguide/github-actions.html
- Android SDK setup: https://github.com/android-actions/setup-android
- GitHub CodeQL: https://github.com/github/codeql-action
