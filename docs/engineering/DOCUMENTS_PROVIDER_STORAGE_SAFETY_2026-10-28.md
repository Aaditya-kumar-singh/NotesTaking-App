# NotesTaking App — DocumentsProvider storage safety (2026-10-28)

**Branch:** `fix/documents-provider-safety`
**Baseline:** `main` at `dcfa122` (split recovery CI passed and merged).
**Scope:** App-owned `Notes` DocumentsProvider only. External SAF providers are outside this implementation.

## Research and risk model
Android [DocumentsProvider API](https://developer.android.com/reference/android/provider/DocumentsProvider) requires correctly reporting ancestry and respecting source/target parent relationships for moves. Google's [path traversal security guidance](https://support.google.com/faqs/answer/7496913?hl=en) calls for checking canonical paths before providing filesystem access. The original provider accepted unexpected ID prefixes, assembled relative filenames without canonical containment, allowed destructive root operations, ignored the supplied source parent during move, and recursively copied without bounds or cleanup.

## Implemented changes
- Introduced `SafeDocumentTree.kt`, a JVM-testable boundary mapping only `root` or `root/<validated segments>` to canonical files inside the private Notes root. Dot segments, slash/backslash names, NUL/control characters, and symlinks are rejected.
- All provider read, query, create, delete, rename, move, and copy operations use the boundary. Root deletion, rename, move, and copy are blocked. Creating under a missing parent or file is blocked; directory traversal and descendant relationships are validated by actual resolved paths rather than string prefixes.
- `moveDocument` checks `sourceParentDocumentId` against the file's real parent, refuses moves into itself/descendants, and never deliberately overwrites an existing destination.
- `copyDocument` rejects self/descendant copies, stages content under a random temporary entry in the target directory, enforces a 64-directory depth limit, 10,000 entry limit, and 1 GiB byte budget. The staged tree is removed if copying or publication fails; the original is untouched.
- Document-opening refuses directories. Folder listings exclude symbolic links. Existing document ID naming and duplicate-name convention are otherwise preserved.

## Tests
`SafeDocumentTreeTest.kt` covers invalid paths and prefix traversal, blocking destructive root operations, bad file names, missing parent, duplicate-name creation, move parent verification, copying into itself or descendants, valid copy/move roundtrips, ancestry checking, symlinks when the test filesystem supports them, and deep-copy rollback. Because this helper uses plain JVM filesystem APIs, tests do not require mocking Android.

## Release gates and limitations
- The structural source scanner is not a Kotlin compiler. GitHub Android CI must pass unit tests, lint, and APK assembly on this branch before merging.
- A live Android SAF instrumentation test remains outstanding (no `app/src/androidTest` suite in the current repository). Check `DocumentsContract` URI grants, rename/move/delete behavior, provider fault injection, recovery after low-storage and read-only directory errors, performance and responsiveness on tablets.
- Staged directory copies are not made durable via directory fsync; and a concurrent filesystem writer can still race path checks. The app-owned root is protected by Android app sandbox and MANAGE_DOCUMENTS, but this is **not** a complete multi-process transactional filesystem.
- `deleteDocument` still uses recursive deletion, which can fail partway. A future Trash/undo feature should stage deletion with recovery semantics rather than promise rollback.
- A hard limit on copying very large folders is intentional; adjust based on device benchmarks and explicit product limits rather than silently allowing unbounded I/O.
- Import attack surfaces (ZIP/XML, PDFs, images, clipboard) and backup privacy are separate P0 tasks. This patch does not fix them.

**Do not publish a production release until tests and device-level storage scenarios succeed.**
