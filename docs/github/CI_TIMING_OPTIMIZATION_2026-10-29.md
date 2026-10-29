# GitHub Android CI timing and optimization — 2026-10-29

Baseline: public Actions runs for `Aaditya-kumar-singh/NotesTaking-App`, before ARM64-only development mode. This report records **observed** timings separately from **estimated** savings. Windows is source-inspection / dry-run only; all Android builds and runnable tests must execute on GitHub Actions.

## Observed GitHub runs

| Run | Branch | Total elapsed reported by GitHub | Native ARMv7 build | Native x86-64 build |
| --- | --- | --- | --- | --- |
| [#44](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/40838034510) | main | ~11m12s | ~1m34s | ~1m09s |
| [#45](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/40839314668) | fix/bounded-font-theme-imports | ~16m18s | ~2m19s | ~2m05s |
| [#46](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/40840634185) | fix/bounded-template-image-staging | ~19m53s | ~3m02s | ~2m44s |
| [#47](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/40840732158) | fix/stylus-main-integration | ~15m52s | ~2m21s | ~1m54s |
| [#48](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/40854704833) | feature/modern-paper-ink-foundation | ~12m28s | ~1m41s | ~1m23s |

Approximate ABI durations are derived from timestamped task boundaries in the GitHub job logs; they are not isolated task performance traces. The x86-64 numbers may include nearby task overhead.

All five runs built three native ABIs. On #48 the ARM64 native build took about 1m46s; the other two together added about **3m04s**. On several earlier runs the other two added **4–6 minutes**.

## Optimization implemented

- The batched CI policy already skips full Android builds on ordinary development-branch pushes and draft PRs; ready PRs and manually requested checks still run.
- The default Android Gradle configuration remains **ARM64 + ARMv7 + x86-64**. When GitHub Actions passes `-PciArm64Only=true`, only `arm64-v8a` is built and packaged in the development APK.
- For UI-only ready PR checks and default feature-branch manual checks, CI builds ARM64 and still runs **all Kotlin unit tests, Android lint, APK construction and both JNI-library checks**. PRs that touch native sources, Gradle build configuration, CI workflow, or APK verifier automatically switch to full ABI coverage using the PR merge diff.
- For a `main` branch run or an explicit `full_abis` manual dispatch, CI sets `CI_ARM64_ONLY=false`, builds the original three ABIs and verifies all six library paths. The separate signed-release workflow stays unchanged and full ABI.
- Debug artifact names distinguish `arm64` and `all-abis` so a development APK is not mistaken for a universal release.

**Estimate, not a measured result:** 3–6 minutes less native compilation in typical PR/checkpoint runs based on the historical logs. Overall runtime also depends on Kotlin compilation, dependency restoration, lint, test execution, network and runner load. Actual before/after comparison requires a new GitHub Actions run on the optimized feature head.

## CodeQL Java/Kotlin extraction: avoid a second native APK build

The separate CodeQL workflow was also running `:app:assembleDebug`, rebuilding all three vendored PDFium/JNI architectures just to extract Java/Kotlin. In the [previous successful CodeQL run](https://github.com/Aaditya-kumar-singh/NotesTaking-App/actions/runs/40838034469), the traced Gradle `assembleDebug` phase took about **10m55s** and included `buildCMakeDebug` for ARM64, ARMv7 and x86-64.

The CodeQL build step is now changed to `:app:compileDebugKotlin :app:compileDebugJavaWithJavac` in one Gradle invocation. The CodeQL `java-kotlin` language, manual compilation mode, `security-extended` queries, analysis/upload step, triggers and permissions are **unchanged**. Android CI still executes native builds, tests, lint and APK checks. The pinned SDK/NDK/CMake installation is retained because Gradle may still need native toolchain metadata at configuration time.

**Status:** Source-only YAML/configuration checks passed. The first GitHub CodeQL run with the new command must still verify successful extraction and uploaded results. It is **not yet a measured speed improvement**. If compilation-only loses Kotlin/Java coverage, restore the working `assembleDebug` command and investigate with GitHub logs; do not report an empty analysis as success.

## Cache assessment

The #48 job restored an existing Gradle cache but reported `Cache was read-only`. Its logs explicitly explain that `gradle/actions/setup-gradle@v6` defaults to saving cache changes only on the GitHub **default branch**. The repository currently reports `master` as default while active development is on `main`.

The configured **basic** Gradle cache restores Gradle User Home (dependencies, local Gradle cache) under an exact Gradle-build-file hash. It does **not** preserve the complete multi-ABI CMake output directory across GitHub runner jobs. Merely enabling branch cache writes does not eliminate fresh native compilation and could consume repository cache quota. Changing the GitHub default branch to `main` after checking protections and release workflows will allow `main` to seed shared caches for future branches; it requires repository-admin action. Do not assume it alone will eliminate C++ build time.

## Safety and next validation

1. Native/toolchain changes on a ready PR automatically select a **full-three-ABI** GitHub check. The workflow computes changed native/build paths against the PR merge commit's first parent; if that comparison fails, the job fails instead of silently falling back to ARM64. The `full_abis` manual input is additionally available when the changed workflow exists on the default branch.
2. After the optimization is available on GitHub, manually dispatch **Android CI** from the feature branch and compare its current commit SHA, native task names/durations, `verify_apk.py` results, test XML, lint, and uploaded APK artifact to the baseline. For native-change PRs, confirm the automatic full-ABI selection in the CI logs.
3. UI-only PRs use one ABI; native/build PRs, post-merge `main` and signed releases retain complete native coverage. If branch protections depend on a particular job name, leave `JDK 17 / tests / lint / debug APK` unchanged.
4. The first build after changing `app/build.gradle.kts` may have a colder **basic** Gradle cache. Do not treat that one run alone as a reliable speed benchmark.
5. This patch does not enable native output caching or change pinned tool versions, Kotlin code, file formats, note storage, user data or signed release build configuration.

References: [Android official ABI filter documentation](https://developer.android.com/studio/projects/gradle-external-native-builds), [Gradle Actions caching guidance](https://github.com/gradle/actions/blob/main/docs/setup-gradle.md), [GitHub workflow dispatch](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow).
