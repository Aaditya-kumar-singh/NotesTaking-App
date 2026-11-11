# NotesTaking App + Notein: hybrid product and integration contract

Recorded: 2026-11-11
Working integration branch: developer
Reference: Notein_Official.apk v1.2.607.0; evidence stored in E:\Notein_ReverseEngineering
Work tracker: FEATURE_MATRIX.md (110 NP IDs); tests: TEST_PLAN.md (79 NT IDs).

## Hybrid feature rule

- **NotesTaking-only features:** preserve them, including files, settings, notes, UI navigation and every working capability.
- **Notein-only features:** independently implement the useful observed interaction using our app's Kotlin/Jetpack Compose, original styling and data model. Add acceptance tests.
- **Both apps:** use Notein's demonstrated workflow, discoverability, input consistency, ergonomics and perceived smoothness as a reference; keep the stronger existing NotesTaking behavior wherever it is more reliable, accessible or complete.
- **Same feature, different algorithms:** retain NotesTaking's editable native stroke architecture, safe storage, undo, and PDF source. Independently optimize the algorithm; do not copy Notein's proprietary/decompiled code, assets, logos, fonts or identifiers.
- **Unverified reference behavior:** mark INVESTIGATE rather than claiming parity or speed. The APK reports do not establish actual S Pen tablet latency or reliable export/recovery outcomes.
- **AI, account, sync, paywall:** stay separately marked optional until user chooses a privacy and infrastructure design; offline note-taking never requires an account.

Never delete an existing feature to mimic another app. Compare outcomes before rewriting code. Preserve .xnote, .xcanvas, .xtemplate, original PDFs and older settings.

## UX and smoothness bar

- Stylus/eraser input has priority over scroll/zoom and toolbars; pen contact owns the gesture until up/cancel. Finger pan versus finger ink is an explicit reversible control. No phantom ink, palm-triggered scroll or missed last stroke.
- Tool and reading mode changes preserve the same canvas/PDF instance, page/zoom/scroll and in-progress state; avoid full-page rebuilds.
- Erasing repaints only a correct dirty region, including caps/brush widths; full dirty bounds remain for cases where geometry requires them. Undo/redo restores the same strokes and fragments.
- Use Android Material3, existing theme and icon tokens, 48dp touch targets, spoken active states, system Back, insets, large fonts, compact/expanded screen layouts and reduced motion.
- Render and input timing must be *measured* on real hardware, not assumed from static Notein code or a passing GitHub build. See PERFORMANCE_ACCEPTANCE.md.

## Commit and verification policy

1. Each B/G batch gets its own local implementation branch and descriptive commit(s), with NP IDs and test coverage. Do not squash.
2. Commit completed batches on the developer lineage after source checks. Publish coherent checkpoints to developer; verify GitHub remote SHA and Kotlin tests, lint, APK.
3. GitHub CI may run while a different batch is worked on locally. Do not push a newer developer head while its prior verification is unresolved.
4. Keep main and master stable during development. Device testing can remain the final pending gate as requested, but source compatibility, tests, static safety and native build correctness must pass.
5. Every feature status is recorded in PROGRESS.md and FEATURE_MATRIX.md. Existing SOURCE/PARTIAL entries do not count as done without evidence.

## Final master replacement gate (future, not authorized to skip checks)

Once all agreed **in-scope** features and bug fixes are completed in developer, and non-device checks pass on the exact final source:

- Review everything still unique to main/master: tablet PDF behavior, eraser fixes, import/recovery, CI, signing, all-ABI and release workflows. Bring necessary fixes into developer before replacement.
- Create rollback refs/tags pointing to the original master and preserve versioned release artifacts. Review branch protection, signatures and workflow dependencies.
- Reconcile the source and release build pipeline. Use a reviewed integration to make master application source equivalent to developer; avoid force push/history loss and never silently delete release automation.
- Run exact-SHA tests, lint, APK validation, output-file tests, compatibility and diff review, then verify remote master SHA. Physical-device QA may be explicitly pending and reported as a risk, not described as complete.
- If protection/release safety blocks the replacement, stop and report the blocker rather than bypassing it.

This agreement does not promote master now. Keep the historical audit findings open until verified.
