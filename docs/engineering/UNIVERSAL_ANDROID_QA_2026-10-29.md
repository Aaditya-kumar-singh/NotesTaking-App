# Universal Android debug APK — device compatibility check

**Branch:** `qa/all-latest-universal-apk`, based on the latest combined implemented features at `b74e5ad`.
**Goal:** Make one test APK compatible with the app's supported Android processor architectures, instead of distributing an ARM64-only QA artifact.

## Build rule

The Android CI workflow now selects **all native ABIs by default for `qa/` branches**. Ordinary feature branches retain fast ARM64-only builds; manual `full_abis` and native/build-change detection still force all ABIs. This is a QA-only workflow change, not a modification of the application or its resources.

The Gradle application already declares these debug-APK architectures:
- `arm64-v8a` (64-bit ARM phones and tablets)
- `armeabi-v7a` (32-bit ARM phones and tablets)
- `x86_64` (64-bit x86 Android devices and emulators)

The minimum Android version is **Android 8.0 (API 26)**. The debug package ID is `com.notestaking.app.debug`. This build **cannot install on Android 7.1 or older**, and does not add `x86` 32-bit support.

## Gate before sharing the APK as verified

1. GitHub Android CI on the exact QA commit must pass project checks, backup privacy, Kotlin unit tests, Android lint and debug APK assembly.
2. The APK verifier must confirm the expected **all-ABI** `libnotestakingpdf.so` and `libnotestakingts.so` libraries and the expected debug package name. Confirm the artifact name includes `debug-all-abis`, not `debug-arm64`.
3. Extract the APK from GitHub's artifact ZIP; verify it opens as an APK archive and includes `classes.dex`, `AndroidManifest.xml` and ABI-specific native libraries. Provide the exact SHA-256 checksum.
4. On a phone, get the exact Android version, device model and installer error if the universal APK still fails. A different signing certificate from an older debug build can cause an **update conflict**, which ABI changes alone cannot solve.

**Safety:** The new debug build is for disposable test notes only. Back up any existing work before uninstalling an old debug build to resolve an installation-signature conflict. No device acceptance or production signing is implied.
