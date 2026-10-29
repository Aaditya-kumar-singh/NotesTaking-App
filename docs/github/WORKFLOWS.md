# NotesTaking App — GitHub automation

This directory documents the active GitHub setup for the Android codebase. Builds and tests run on GitHub Actions; local Windows work is restricted to source inspection and dry-run checks. See `BATCHED_CI_POLICY.md` before pushing feature changes.

## What's automated

| File | When | What it does |
| --- | --- | --- |
| `.github/workflows/android-ci.yml` | Ready PR targeting `main`, manual dispatch, or push to `main` | Draft PRs skip jobs and ordinary feature/fix branch pushes do not trigger builds. A ready PR runs source checks, Kotlin tests, lint and an ARM64 debug APK for UI-only changes; native/build changes automatically run all three ABIs. `main` gets full-three-ABI post-merge verification; manual `full_abis` remains available. |
| `.github/workflows/android-release.yml` | Manual only | Check an existing Git tag against `versionName`, use GitHub Secrets to sign the release APK, verify signature, upload signed APK + SHA-256, optionally create a **draft** GitHub Release. |
| `.github/workflows/dependency-graph.yml` | Main branch Gradle changes, manual | Resolve and submit Gradle dependency graph to GitHub dependency monitoring. |
| `.github/workflows/codeql.yml` | Main Kotlin changes, weekly, manual | Build Kotlin source for CodeQL and upload Java/Kotlin security findings (requires repository code-scanning availability). |
| `.github/dependabot.yml` | Weekly | Open PRs for Gradle and GitHub Actions dependency updates. |
| `.github/ISSUE_TEMPLATE/` and `.github/pull_request_template.md` | GitHub UI | Standardize issue reports, feature requests, changes, testing, and compatibility checks. |

## One-time GitHub configuration

1. The repository is `Aaditya-kumar-singh/NotesTaking-App`, not the old XNotes upstream. Keep its remote and attribution intact.
2. Git pushes are performed from the configured SSH remote. Keep integration work on `main` and avoid directly pushing unverified feature changes there.
3. In your repo's **Settings → Actions → General**, enable workflows and set the default `GITHUB_TOKEN` to read-only where the UI offers that option. The release and dependency graph workflows request the specific write permission they need.
4. As of this policy update, GitHub still reports `master` as its default branch; the active integration branch is `main`. Change the default to `main` separately after reviewing release and protection settings; the batched Android CI PR trigger targets `main`.
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

Follow the batch policy in `BATCHED_CI_POLICY.md`: edit and dry-run locally, commit related changes in a feature branch, push when the batch needs review, and optionally open a **draft** PR. Neither an ordinary feature branch push nor a draft PR starts the Android build. When the batch is ready, mark its PR **Ready for review** (or create a non-draft PR) to run the full GitHub checks. Subsequent pushes to a ready PR rerun CI for its latest commit. Merge to `main` only after green checks and review; a `main` push performs post-merge verification. Dependabot continues opening dependency PRs separately.

For a manual batch verification, open **Actions → Android CI → Run workflow**, select the **feature branch** under `Use workflow from`, and run it after the final push. The checked SHA must match the batch being reviewed. Debug APK artifacts use application ID `com.notestaking.app.debug`; they are not production releases.

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

The following Gradle command runs **only on GitHub Actions**, not on the Windows development machine:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Local source-only checks do not compile the application. Actual Kotlin tests, lint, native compilation, APK verification and artifacts run on GitHub Actions only. The scripts under `tools/ci/` are shared with the GitHub runner.

## References

- GitHub Actions documentation: https://docs.github.com/actions
- Gradle on GitHub Actions: https://docs.gradle.org/current/userguide/github-actions.html
- Android SDK setup: https://github.com/android-actions/setup-android
- GitHub CodeQL: https://github.com/github/codeql-action
