## Summary
Describe what changed in NotesTaking App.

## Issue / motivation
Link the GitHub issue or explain the problem.

## Verification
- [ ] `python3 tools/ci/check_project.py`
- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] `./gradlew :app:assembleDebug`
- [ ] `./gradlew :app:lintDebug`
- [ ] Tested on an emulator or physical Android device (or explain why not)

## Important app checks
- [ ] Existing `.xnote`, `.xcanvas`, and `.xtemplate` files remain readable
- [ ] Save, recovery and import/export behavior checked where relevant
- [ ] PDFium and Tree-sitter native names/loads unaffected (or verified)
- [ ] No credentials, private notes, keystores or generated audit logs added
- [ ] Third-party licenses and original MIT attribution preserved

## Screenshots / evidence
Attach sanitized screenshots or relevant CI artifacts.

## Risks / rollback
Explain any potential data loss, migration, or security impact.
