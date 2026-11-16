# REL-18 — Release version, tag and signing continuity preflight

**Date:** 2026-11-16
**Scope:** Source-only coordinator review. No signing keys inspected, release created or CI executed.

## Observed release identity

| Property | Current source |
|---|---|
| Package ID | `com.notestaking.app` |
| Gradle versionName | `0.1.0` |
| Gradle versionCode | `1` |
| Signed-release tag required by `check_release_tag.py` | `v0.1.0` |
| Existing remote versioned tags | `v0.1.0-beta.1`, `v0.1.0-beta.2`, `v0.1.0-beta.3` |
| Remote `v0.1.0` tag | **Absent** as of audit |
| Supported production APK ABIs by default | `arm64-v8a`, `armeabi-v7a`, `x86_64` |
| Required release environment | `release` |

`v0.1.0-beta.3` points to historical `master` commit `13fbb60b605049f8864c365da1b26f774181dd4e`. Previous beta workflows distribute `com.notestaking.app.debug` test packages signed with a debug key. Neither the beta package identity nor its debug certificate is evidence of continuity for `com.notestaking.app`.

The current `tools/ci/check_release_tag.py` accepts `v0.1.0` against `versionName` but rejects existing `v0.1.0-beta.3`. The separate Git tag existence check rejects missing `v0.1.0`. Do not create a new version tag or change `versionCode` until a release candidate and prior-install upgrade path are approved.

## Workflow protections prepared locally

1. Reapplied the previously reviewed exact-tag checkout correction from original local commit `a8b8ef935f111a6049d76e06fd90b4f82356bc9b`. The signed-release workflow now checks out `refs/tags/${{ inputs.tag }}` and explicitly verifies `HEAD` equals the peeled tag commit. The corresponding cherry-pick commit in this branch is `d0c81af`.
2. Added a **fail-closed release certificate check**: the `release` GitHub environment must provide the public variable `ANDROID_RELEASE_CERT_SHA256` (64 hex characters, optionally colon separated). Before decoding the signing keystore, the workflow validates that fingerprint is configured. After signing, it checks that the single signer reported by `apksigner verify --print-certs` matches the normalized approved fingerprint. A missing/mismatched/multiple certificate stops artifact upload and draft publication.
3. Kept existing package/JNI validation, sha256sum, signed APK artifact and draft-only GitHub Release behavior. Existing signing files are removed by the workflow's `if: always()` cleanup step.

**Signing material was not accessed or altered.** The workflow references four required GitHub secrets by name: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`. Existence of these secrets, release environment protection rules and the correctness of the keystore contents are **unverified**. The new fingerprint variable is a public key identifier, not a password/private key. An authorized release owner must establish its trusted value from the approved **production signing certificate**, not a debug beta APK, and set it in the protected `release` environment.

## Release approval blockers

- No release candidate tag `v0.1.0`; release branch, source SHA and versionCode upgrade policy are not approved. The workflow fixes are **local only** and are not in the default GitHub workflow definition yet.
- No trusted production certificate fingerprint or proof that signing keystore secrets correspond to it; no verified previous signed `com.notestaking.app` APK for comparison.
- No exact-tag release signing run, `apksigner` result, full-ABI GitHub CI on the candidate SHA or downloaded APK hash/signature check.
- Device acceptance cards REL-08 through REL-13 and release gates REL-14, REL-16, REL-19, REL-20 are still open. A production package must not be published from this source-only audit.
- Rollback references prepared during REL-17 are local. Before promotion, publish an approved protected remote rollback reference and verify it separately.

## Checks performed

- Parsed the workflow as YAML and asserted the exact-tag checkout, tag-to-HEAD equality, nonempty expected-certificate variable and single-signer digest comparison logic.
- Ran `python tools/ci/check_release_tag.py v0.1.0` (versionName matches); existing `v0.1.0-beta.3` fails expectedly against the current source version.
- Ran repository structure, privacy and A–Z catalog checks, and Git whitespace validation. No local Gradle/Kotlin/Android build or certificate extraction was attempted.

**Disposition:** REL-18 **local source-audit ready**, not release verified. Keep `developer`, `main`, `master`, tags and signing settings unchanged until an authorized candidate, approved signing certificate, full-ABI CI and real-device evidence are available.
