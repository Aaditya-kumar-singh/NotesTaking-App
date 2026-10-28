# Import archive and XML security — implementation checkpoint (2026-10-28)

**Branch:** `fix/import-archive-xml-safety` based on verified storage-security branch `ad7d90a`. This is source/CI work, not yet device-tested or production-certified.

## Research and problems confirmed in current code

- `DocumentCodec.read` and `CanvasCodec.read` used unbounded `ZipInputStream` extraction to temporary files; `DocumentCodec` used `readBytes()` for embedded `flow.xml`. Unknown entries could also be consumed without a limit.
- `FlowXml.readInto` parsed untrusted XML using default `DocumentBuilderFactory`, which does not guarantee DOCTYPE/XXE protection. `SvgReader` and `MarkupsXfdf` only applied some parser options opportunistically.
- `TemplatePackage.extract` used unbounded copying directly into the final output file. An inflated image could use substantial disk space or replace the previous contents before failing.
- Malformed archive processing could leave temporary images, PDFs, audio and template files after a partial read.

OWASP's [XXE Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html) recommends **rejecting DOCTYPE** and stopping if mandatory parser protection is unsupported; disable external entity resolution and XInclude. The implementation uses that policy.

## Implemented

1. **BoundedZipInputStream** used by both native codecs for normal reads and stream peeks. Rejects path-traversal names, invalid path segments, duplicate entry names, more than 10,000 entries, more than 1 GiB total inflated bytes and excessive individual entry sizes. Limits are: manifest 64 MiB, embedded PDF 512 MiB, images 64 MiB, audio 256 MiB, XML 16 MiB, other entries 32–64 MiB. Counts inflated bytes during normal reads and `closeEntry` of skipped entries; does not trust declared ZIP entry sizes.
2. **SecureXml** centralizes DOM parser configuration: mandatory DOCTYPE rejection and secure processing, external entity resolver that always throws, no entity expansion, XInclude disabled wherever supported, and extra JAXP external-access restrictions when available. `FlowXml`, `MarkupsXfdf` and `SvgReader` share it.
3. **TemplatePackage** validates archive picture paths, limits compressed package size to 16 MiB and 1,000 entries, refuses images above 32 MiB **inflated**, synchronizes writes, and atomically publishes a fully extracted staged file. Failure leaves the previously committed destination unmodified.
4. **Rollback on failed native-note import:** source PDF, embedded images, audio and template files created by a failing codec read are deleted rather than abandoned.

## New regression tests

- `BoundedZipInputStreamTest`: normal manifest/image reading, invalid entry paths, oversize inflated XML, skipped-entry drain limit, entry-count cap.
- `SecureXmlTest`: valid XML, rejected external entity and inline DTD expansion.
- `TemplatePackageTest`: oversized image extraction preserving prior output, invalid path and control-character names.
- `ImportCleanupTest`: malformed ZIP after a streamed image leaves no temporary note or canvas images.

## Remaining release gates

- Must pass GitHub Android CI unit tests, lint and APK build on the feature branch. Do not merge until green.
- Android device tests are required for legacy .xnote/.xcanvas/.xtemplate and very large legitimate user notes; budgets are conservative and may need adjustment based on observed real-world note sizes.
- The budget does not provide a full compressed-to-inflated ratio or CPU-time governor; if a 1 GiB extraction is too costly for low-end tablets, use lower per-device or per-operation policy.
- Verify all native PDFium image/PDF rendering decoders and direct untrusted PDF/import streams separately; this patch is not a complete malformed-PDF defense.
- `ZipTail` random-access previews, external provider fault injection, and document-parser object-count limits need a separate audit. XML parsing may still require DOM node-depth limits in addition to byte caps for extreme documents.
- User-visible errors should distinguish corrupt, oversized and unsupported notes. Existing older saved notes should remain readable inside these limits.
