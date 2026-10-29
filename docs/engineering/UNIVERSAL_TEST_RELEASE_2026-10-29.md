# Universal Android Beta 2 — GitHub distribution checklist

## Goal

Publish the **already verified** all-ABI Android test APK as a persistent public GitHub
**prerelease** so a phone can download the `.apk` directly without downloading an
expiring GitHub Actions ZIP or using a chat attachment.

- Tag: `v0.1.0-beta.2` (new, must not overwrite beta.1).
- Exact GitHub CI run: `40918820050` (completed successfully).
- Exact app source from CI: `b36c1f941abe8f55331f8086aef6e26f4b0e6e90`.
- Original artifact: `NotesTakingApp-debug-all-abis-66`.
- Release APK name: `NotesTakingApp-Universal-v0.1.0-beta.2-debug.apk`.
- Install identity: `com.notestaking.app.debug`, Android API 26+, native ABIs
  `armeabi-v7a`, `arm64-v8a` and `x86_64`.

## Safeguards

1. Workflow runs only on the new tag, from a separate release/distribution branch;
   no changes to `main`, `master` or the original QA PR branches.
2. The tag's project source must match the exact SHA of the completed GitHub Android
   CI run, except this release workflow and this distribution document.
3. GitHub REST API must confirm that source commit and successful CI conclusion
   before the artifact may be republished.
4. The extracted APK must have valid ZIP CRC, Android DEX, a manifest and both
   native libraries for all three ABIs, or the release job fails.
5. Publish only a debug **prerelease**, with a SHA-256 checksum and appropriate
   testing and signing warnings. No signed production build or auto-promotion.

## Post-run acceptance

- [ ] GitHub release workflow triggered from tag `v0.1.0-beta.2`.
- [ ] All workflow steps, including verification and `gh release create`, passed.
- [ ] GitHub REST confirms `prerelease: true` and both APK and SHA-256 assets.
- [ ] Direct public GitHub release asset link points to actual APK.
- [ ] User tests on their Android device and reports the exact installation
      failure if one persists; physical stylus and data-safety QA is pending.

Historical audit findings remain open until supported by individual regression
and device validation. Older beta.1 remains available and unchanged.
