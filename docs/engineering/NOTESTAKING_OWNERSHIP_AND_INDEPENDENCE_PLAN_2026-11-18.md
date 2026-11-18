# NotesTaking App: ownership and independent-code plan

**Maintainer and developer:** Aaditya Kumar
**GitHub:** https://github.com/Aaditya-kumar-singh/NotesTaking-App
**Scope:** Original NotesTaking App changes and product branding. The existing codebase contains MIT-licensed XNotes code and separately licensed third-party material.

## Current ownership

Aaditya Kumar maintains the NotesTaking App edition and claims copyright in his original contributions and branding. The original XNotes copyright remains with its original copyright holder; see `LICENSE` and `NOTICE.md`. Vendored PDFium, Tree-sitter, fonts, artwork and other third-party components retain their own terms.

Changing package names, comments, commit authors, repository ownership, or a license header does not transfer copyright. Keep legally required notices with distributed copies, including APKs where applicable.

## Path to an entirely independently authored application

1. **Establish rights.** Obtain a written copyright assignment or separate authorization for any code whose ownership must transfer, or replace the XNotes-derived implementation with independently written code. Don't copy the old implementation into a new project and call it original.
2. **Rebuild from features and tests.** Write a fresh specification for note editing, Canvas drawing, saving, PDF import/export, toolbars, recovery, accessibility, and compatibility. Independently implement these parts and document code authorship.
3. **Keep users' data safe.** Preserve import and export for `.xnote`, `.xcanvas`, `.ntcanvas` and `.xtemplate` where required. Serialized `xnotes:` identifiers and native recovery markers are compatibility contracts, not public branding. Validate updates using disposable notes and real Android devices before migrating files.
4. **Clear third-party material.** Replace third-party artwork where exclusive copyright is required. Use third-party libraries only under their documented licenses or implement alternatives. Every replacement needs a provenance record.
5. **Control release assets.** Verify GitHub administration, approved production signing key, release environment, package ownership, store account, backup/export path, and rollback process.
6. **Verify the new code.** Run Kotlin tests, Android lint, native builds, security checks, and physical-device save/reopen/export tests on the exact source commit. Don't publish an unsigned or untested candidate as production-ready.

## Do not delete

- The original `LICENSE` or required XNotes attribution from this derivative codebase.
- PDFium, Tree-sitter, font, icon and other vendor licenses.
- Legacy document formats or storage identifiers simply to remove an old name.
- Git history, user notes or QA fixtures without an approved migration and backup.

**Status, 18 November 2026:** This change updates first-party ownership wording and localization. It does not complete an independent rewrite or transfer third-party copyright.
